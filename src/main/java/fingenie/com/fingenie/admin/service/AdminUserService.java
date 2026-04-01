package fingenie.com.fingenie.admin.service;

import fingenie.com.fingenie.admin.dto.*;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Transaction;
import fingenie.com.fingenie.entity.UserProfile;
import fingenie.com.fingenie.entity.Wallet;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.TransactionRepository;
import fingenie.com.fingenie.repository.UserProfileRepository;
import fingenie.com.fingenie.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin service for user management operations.
 *
 * <h3>Status derivation</h3>
 * The Account entity has no explicit status column.  Status is derived:
 * <ul>
 *   <li>BANNED      – {@code isActive = false}</li>
 *   <li>INACTIVE    – {@code isActive = true && isDeleted = true}</li>
 *   <li>PENDING_KYC – {@code isActive = true && !isDeleted && !emailVerified}</li>
 *   <li>ACTIVE      – {@code isActive = true && !isDeleted && emailVerified}</li>
 * </ul>
 *
 * <h3>Activity log</h3>
 * A persistent {@code LoginLog} table has not yet been added to the schema.
 * {@link #getUserActivity} returns an empty page until that migration lands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository      walletRepository;
    private final UserProfileRepository userProfileRepository;
    private final JavaMailSender        mailSender;

    @Value("${spring.mail.username:noreply@fingenie.com}")
    private String fromEmail;

    @Value("${app.name:FinGenie}")
    private String appName;

    // ── List / search ─────────────────────────────────────────────────────────

    /**
     * Returns a paginated, filtered list of users.
     *
     * @param page        zero-based page index
     * @param size        page size (capped at 200)
     * @param search      case-insensitive partial match on email or name (null = skip)
     * @param status      ACTIVE | INACTIVE | BANNED | PENDING_KYC (null = all)
     * @param role        Account.Role filter (null = all)
     * @param createdFrom inclusive lower bound on createdAt (null = skip)
     * @param createdTo   inclusive upper bound on createdAt (null = skip)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(
            int page, int size,
            String search,
            String status,
            Account.Role role,
            LocalDate createdFrom,
            LocalDate createdTo) {

        int safeSize = Math.min(Math.max(size, 1), 200);
        // Sort is handled inside the native SQL query (ORDER BY a.created_at DESC)
        // Do NOT pass Sort here — Hibernate appends camelCase column names for native queries
        PageRequest pageable = PageRequest.of(page, safeSize);

        Timestamp from = createdFrom != null
                ? Timestamp.valueOf(createdFrom.atStartOfDay()) : null;
        Timestamp to = createdTo != null
                ? Timestamp.valueOf(createdTo.atTime(23, 59, 59)) : null;

        // Pre-compute search pattern to avoid LOWER(CONCAT(bytea)) issue in PostgreSQL
        String searchParam = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%" : null;

        Integer roleParam = role != null ? role.ordinal() : null;

        Page<Account> accountPage = accountRepository.adminSearch(
                searchParam, roleParam, status, from, to, pageable);

        return accountPage.map(this::toUserResponse);
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    /**
     * Returns the full detail view of one user.
     *
     * @throws EntityNotFoundException if no account with the given ID exists
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        Account account = requireAccount(userId);

        BigDecimal totalBalance = walletRepository.sumBalanceByAccountId(userId);
        long       txnCount     = transactionRepository.countTotalByAccountId(userId);
        BigDecimal totalEarned  = transactionRepository.sumTotalIncomeByAccountId(userId);
        BigDecimal totalSpent   = transactionRepository.sumTotalExpenseByAccountId(userId);

        String phone = userProfileRepository.findByAccountId(userId)
                .map(UserProfile::getPhoneNumber)
                .orElse(null);

        return AdminUserDetailResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .name(account.getName())
                .phone(phone)
                .status(deriveStatus(account))
                .kycStatus(deriveKycStatus(account))
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .totalTransactions(txnCount)
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                // detail-only fields
                .profilePicture(account.getAvatarUrl())
                .joinDate(account.getCreatedAt())
                .lastActivityAt(account.getLastLogin())
                .reportsCount(0L)   // no ReportLog entity yet
                .warningsCount(0L)  // no WarningLog entity yet
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .totalEarned(totalEarned != null ? totalEarned : BigDecimal.ZERO)
                .build();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Applies a partial update to an account.  Only non-null fields in the
     * request are applied.
     */
    @Transactional
    public AdminUserDetailResponse updateUser(Long userId, AdminUpdateUserRequest req) {
        Account account = requireAccount(userId);

        if (req.getName() != null && !req.getName().isBlank()) {
            account.setName(req.getName().trim());
        }
        if (req.getRole() != null) {
            account.setRole(req.getRole());
        }
        if (req.getIsPremium() != null) {
            account.setPremium(req.getIsPremium());
        }
        if (req.getEmailVerified() != null) {
            account.setEmailVerified(req.getEmailVerified());
        }

        accountRepository.save(account);
        log.info("[AdminUserService] Updated user {} by admin", userId);

        return getUserDetail(userId);
    }

    // ── Ban / Restore ─────────────────────────────────────────────────────────

    /**
     * Bans a user by setting {@code isActive = false}.
     * The reason is written to the application log (no ban-reason column exists).
     *
     * @throws IllegalStateException    if the account is already banned
     * @throws EntityNotFoundException  if no account exists
     */
    @Transactional
    public void banUser(Long userId, String reason) {
        Account account = requireAccount(userId);
        if (!account.isActive()) {
            throw new IllegalStateException("User " + userId + " is already banned");
        }
        account.setActive(false);
        accountRepository.save(account);
        log.warn("[AdminUserService] User {} BANNED. Reason: {}", userId, reason);
    }

    /**
     * Restores a previously banned account by setting {@code isActive = true}.
     *
     * @throws IllegalStateException   if the account is not banned
     * @throws EntityNotFoundException if no account exists
     */
    @Transactional
    public void restoreUser(Long userId) {
        Account account = requireAccount(userId);
        if (account.isActive()) {
            throw new IllegalStateException("User " + userId + " is not banned");
        }
        account.setActive(true);
        accountRepository.save(account);
        log.info("[AdminUserService] User {} RESTORED by admin", userId);
    }

    // ── Activity (login history) ──────────────────────────────────────────────

    /**
     * Returns paginated login activity for a user.
     *
     * <p><b>Note:</b> A LoginLog entity/table has not yet been added to the
     * schema.  This method always returns an empty page.  Add a
     * {@code LoginLog} entity and repository, then replace this stub.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserActivityResponse> getUserActivity(Long userId, int page, int size) {
        requireAccount(userId); // validate existence
        // TODO: replace with LoginLog query once the entity is created
        return Page.empty(PageRequest.of(page, Math.max(size, 1)));
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * Returns paginated transactions for a single user, newest first.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserTransactionResponse> getUserTransactions(
            Long userId, int page, int size) {

        requireAccount(userId);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(page, safeSize);

        return transactionRepository
                .findByAccountIdPaged(userId, pageable)
                .map(this::toUserTransactionResponse);
    }

    // ── Wallets ───────────────────────────────────────────────────────────────

    /**
     * Returns all wallets owned by a user.
     */
    @Transactional(readOnly = true)
    public List<AdminUserWalletResponse> getUserWallets(Long userId) {
        requireAccount(userId);
        return walletRepository.findByAccountId(userId)
                .stream()
                .map(this::toWalletResponse)
                .toList();
    }

    // ── Bulk operations ───────────────────────────────────────────────────────

    /**
     * Bans multiple users by setting {@code isActive = false}.
     * Skips already-banned accounts silently (logs a warning).
     */
    @Transactional
    public void banUsersBulk(List<Long> userIds, String reason) {
        List<Account> accounts = accountRepository.findAllById(userIds);
        for (Account account : accounts) {
            if (!account.isActive()) {
                log.warn("[AdminUserService] Bulk-ban: user {} already banned – skipped", account.getId());
                continue;
            }
            account.setActive(false);
        }
        accountRepository.saveAll(accounts);
        log.warn("[AdminUserService] Bulk-banned {} users. Reason: {}", accounts.size(), reason);
    }

    /**
     * Sends an email to multiple users via the configured SMTP sender.
     * Failures for individual recipients are logged but do not abort the batch.
     */
    public void emailUsersBulk(List<Long> userIds, String subject, String content) {
        List<Account> accounts = accountRepository.findAllById(userIds);
        int sent = 0;
        for (Account account : accounts) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromEmail);
                msg.setTo(account.getEmail());
                msg.setSubject("[" + appName + "] " + subject);
                msg.setText(content);
                mailSender.send(msg);
                sent++;
            } catch (Exception ex) {
                log.error("[AdminUserService] Failed to send bulk email to {}: {}",
                        account.getEmail(), ex.getMessage());
            }
        }
        log.info("[AdminUserService] Bulk email sent to {}/{} users. Subject: {}",
                sent, accounts.size(), subject);
    }

    // ── CSV export ────────────────────────────────────────────────────────────

    /**
     * Exports all users as a UTF-8 CSV byte array.
     * Columns: id, email, name, role, status, kycStatus, createdAt, lastLogin
     */
    @Transactional(readOnly = true)
    public byte[] exportUsersToCsv() {
        List<Account> accounts = accountRepository.findAll(
                Sort.by(Sort.Direction.ASC, "createdAt"));

        StringBuilder csv = new StringBuilder(
                "id,email,name,role,status,kycStatus,createdAt,lastLogin\n");

        for (Account a : accounts) {
            csv.append(a.getId()).append(',')
               .append(escapeCsv(a.getEmail())).append(',')
               .append(escapeCsv(a.getName())).append(',')
               .append(a.getRole() != null ? a.getRole().name() : "").append(',')
               .append(deriveStatus(a)).append(',')
               .append(deriveKycStatus(a)).append(',')
               .append(a.getCreatedAt() != null
                       ? a.getCreatedAt().toLocalDateTime().toLocalDate() : "").append(',')
               .append(a.getLastLogin() != null
                       ? a.getLastLogin().toLocalDateTime().toLocalDate() : "")
               .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Account requireAccount(Long userId) {
        return accountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + userId));
    }

    /**
     * Derives a human-readable status string from Account boolean flags.
     */
    private String deriveStatus(Account account) {
        if (!account.isActive())   return "BANNED";
        if (account.isDeleted())   return "INACTIVE";
        if (!account.isEmailVerified()) return "PENDING_KYC";
        return "ACTIVE";
    }

    private String deriveKycStatus(Account account) {
        return account.isEmailVerified() ? "VERIFIED" : "PENDING";
    }

    private AdminUserResponse toUserResponse(Account account) {
        BigDecimal totalBalance = walletRepository.sumBalanceByAccountId(account.getId());
        long txnCount = transactionRepository.countTotalByAccountId(account.getId());

        String phone = userProfileRepository.findByAccountId(account.getId())
                .map(UserProfile::getPhoneNumber)
                .orElse(null);

        return AdminUserResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .name(account.getName())
                .phone(phone)
                .status(deriveStatus(account))
                .kycStatus(deriveKycStatus(account))
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .totalTransactions(txnCount)
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .build();
    }

    private AdminUserTransactionResponse toUserTransactionResponse(Transaction t) {
        return AdminUserTransactionResponse.builder()
                .transactionId(t.getId())
                .amount(t.getAmount())
                .description(t.getDescription())
                .categoryName(t.getCategory() != null ? t.getCategory().getCategoryName() : null)
                .categoryType(t.getCategory() != null
                        ? t.getCategory().getCategoryType().name() : null)
                .walletName(t.getWallet() != null ? t.getWallet().getWalletName() : null)
                .transactionDate(t.getTransactionDate())
                .build();
    }

    private AdminUserWalletResponse toWalletResponse(Wallet w) {
        return AdminUserWalletResponse.builder()
                .walletId(w.getId())
                .walletName(w.getWalletName())
                .balance(w.getBalance())
                .currency("VND")
                .isDefault(w.isDefault())
                .createdAt(w.getCreatedAt())
                .build();
    }

    /** Wraps a CSV field in double-quotes and escapes internal quotes. */
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
