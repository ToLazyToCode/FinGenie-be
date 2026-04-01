package fingenie.com.fingenie.admin.service;

import fingenie.com.fingenie.admin.dto.AdminDashboardChartsResponse;
import fingenie.com.fingenie.admin.dto.AdminDashboardStatsResponse;
import fingenie.com.fingenie.admin.dto.AdminRecentTransactionResponse;
import fingenie.com.fingenie.admin.dto.AdminSystemHealthResponse;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Transaction;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.TransactionRepository;
import fingenie.com.fingenie.review.entity.ReviewStatus;
import fingenie.com.fingenie.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ReviewRepository reviewRepository;

    // ── KPI stats ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getStats() {
        long activeUsers = accountRepository.countActiveAccounts();

        Timestamp startOfToday = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        long newUsersToday = accountRepository.countNewAccountsSince(startOfToday);

        BigDecimal totalIncome = transactionRepository.sumAllIncome();
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;

        long totalTransactions = transactionRepository.count();
        long totalReviews = reviewRepository.countByIsDeletedFalse();
        long pendingReviews = reviewRepository.countByStatusAndIsDeletedFalse(ReviewStatus.PENDING);

        return AdminDashboardStatsResponse.builder()
            .totalActiveUsers(activeUsers)
            .newUsersToday(newUsersToday)
            .totalIncome(totalIncome)
            .totalTransactions(totalTransactions)
            .totalReviews(totalReviews)
            .pendingReviews(pendingReviews)
            .build();
    }

    // ── Charts (last 30 days) ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminDashboardChartsResponse getCharts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Timestamp since = Timestamp.valueOf(thirtyDaysAgo);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Build ordered label list (last 30 days)
        Map<String, Long> userGrowth = new LinkedHashMap<>();
        Map<String, Long> txnVolume = new LinkedHashMap<>();

        for (int i = 29; i >= 0; i--) {
            String label = LocalDate.now().minusDays(i).format(formatter);
            userGrowth.put(label, 0L);
            txnVolume.put(label, 0L);
        }

        // Aggregate new users
        List<Account> recentAccounts = accountRepository.findAccountsCreatedSince(since);
        for (Account a : recentAccounts) {
            if (a.getCreatedAt() != null) {
                String day = a.getCreatedAt().toLocalDateTime().format(formatter);
                userGrowth.computeIfPresent(day, (k, v) -> v + 1);
            }
        }

        // Aggregate transactions (we only have global count; group by date using JPA findAll)
        List<Transaction> recentTxns = transactionRepository.findAll(
            PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "transactionDate"))
        ).getContent();

        for (Transaction t : recentTxns) {
            if (t.getTransactionDate() != null) {
                String day = t.getTransactionDate().toLocalDate().format(formatter);
                txnVolume.computeIfPresent(day, (k, v) -> v + 1);
            }
        }

        return AdminDashboardChartsResponse.builder()
            .userGrowthByDay(userGrowth)
            .transactionVolumeByDay(txnVolume)
            .labels(List.copyOf(userGrowth.keySet()))
            .build();
    }

    // ── Recent transactions ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminRecentTransactionResponse> getRecentTransactions(int limit) {
        List<Transaction> page = transactionRepository.findAll(
            PageRequest.of(0, Math.min(limit, 100),
                Sort.by(Sort.Direction.DESC, "transactionDate"))
        ).getContent();

        return page.stream()
            .map(t -> AdminRecentTransactionResponse.builder()
                .transactionId(t.getId())
                .accountId(t.getAccount() != null ? t.getAccount().getId() : null)
                .accountEmail(t.getAccount() != null ? t.getAccount().getEmail() : null)
                .accountName(t.getAccount() != null ? t.getAccount().getName() : null)
                .amount(t.getAmount())
                .description(t.getDescription())
                .categoryName(t.getCategory() != null ? t.getCategory().getCategoryName() : null)
                .transactionDate(t.getTransactionDate() != null
                    ? t.getTransactionDate().toLocalDate() : null)
                .build())
            .toList();
    }

    // ── System health ─────────────────────────────────────────────────────────

    public AdminSystemHealthResponse getSystemHealth() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long usedBytes = memBean.getHeapMemoryUsage().getUsed();
        long maxBytes  = memBean.getHeapMemoryUsage().getMax();

        long usedMb = usedBytes / (1024 * 1024);
        long maxMb  = maxBytes  / (1024 * 1024);
        double pct  = maxBytes > 0 ? (usedBytes * 100.0 / maxBytes) : 0.0;

        int activeThreads = ManagementFactory.getThreadMXBean().getThreadCount();
        long uptimeSec    = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        // Simple DB ping
        String dbStatus;
        try {
            accountRepository.count();
            dbStatus = "UP";
        } catch (Exception ex) {
            log.warn("[AdminDashboard] DB health check failed", ex);
            dbStatus = "DOWN";
        }

        String overallStatus = "DOWN".equals(dbStatus) ? "DEGRADED" : "UP";

        return AdminSystemHealthResponse.builder()
            .status(overallStatus)
            .databaseStatus(dbStatus)
            .heapUsedMb(usedMb)
            .heapMaxMb(maxMb)
            .heapUsagePercent(Math.round(pct * 10.0) / 10.0)
            .activeThreads(activeThreads)
            .uptimeSeconds(uptimeSec)
            .build();
    }
}
