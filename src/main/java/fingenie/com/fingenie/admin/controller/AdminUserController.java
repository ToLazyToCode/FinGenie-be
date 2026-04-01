package fingenie.com.fingenie.admin.controller;

import fingenie.com.fingenie.admin.dto.*;
import fingenie.com.fingenie.admin.service.AdminUserService;
import fingenie.com.fingenie.entity.Account;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin endpoints for User Management.
 * All endpoints require the ADMIN role (also enforced at the filter-chain level).
 *
 * <pre>
 * GET  /api/v1/admin/users                       – paginated list with filters
 * GET  /api/v1/admin/users/{userId}               – full user detail
 * PUT  /api/v1/admin/users/{userId}               – partial update
 * POST /api/v1/admin/users/{userId}/ban           – ban a user
 * POST /api/v1/admin/users/{userId}/restore       – restore a banned user
 * GET  /api/v1/admin/users/{userId}/activity      – login history (paginated)
 * GET  /api/v1/admin/users/{userId}/transactions  – transaction history (paginated)
 * GET  /api/v1/admin/users/{userId}/wallets       – user's wallets
 * POST /api/v1/admin/users/bulk-ban               – ban multiple users
 * POST /api/v1/admin/users/bulk-email             – email multiple users
 * GET  /api/v1/admin/users/export                 – CSV export
 * </pre>
 */
@RestController
@RequestMapping("${api-prefix}/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    // ── List ──────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users?page=0&size=20&search=alice&status=ACTIVE&role=USER
     *                         &createdFrom=2024-01-01&createdTo=2024-12-31
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 20, max 200)
     * @param search      partial match on email or name
     * @param status      ACTIVE | INACTIVE | BANNED | PENDING_KYC
     * @param role        USER | MODERATOR | ADMIN
     * @param createdFrom ISO date lower bound for createdAt (inclusive)
     * @param createdTo   ISO date upper bound for createdAt (inclusive)
     */
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    Account.Role role,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {

        return ResponseEntity.ok(
                adminUserService.getUsers(page, size, search, status, role, createdFrom, createdTo));
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users/{userId}
     * Returns full user detail including spending analytics and wallet summary.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUserDetail(userId));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * PUT /api/v1/admin/users/{userId}
     * Partially updates mutable account fields (name, role, isPremium, emailVerified).
     * Only non-null request fields are applied.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody AdminUpdateUserRequest request) {

        return ResponseEntity.ok(adminUserService.updateUser(userId, request));
    }

    // ── Ban / Restore ─────────────────────────────────────────────────────────

    /**
     * POST /api/v1/admin/users/{userId}/ban
     * Body: { "reason": "Suspicious activity" }
     */
    @PostMapping("/{userId}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminBanUserRequest request) {

        adminUserService.banUser(userId, request.getReason());
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/admin/users/{userId}/restore
     * Restores an account that was previously banned.
     */
    @PostMapping("/{userId}/restore")
    public ResponseEntity<Void> restoreUser(@PathVariable Long userId) {
        adminUserService.restoreUser(userId);
        return ResponseEntity.ok().build();
    }

    // ── Activity (login history) ──────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users/{userId}/activity?page=0&size=20
     * Returns paginated login history.
     * Currently returns an empty page until a LoginLog entity is added.
     */
    @GetMapping("/{userId}/activity")
    public ResponseEntity<Page<AdminUserActivityResponse>> getUserActivity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(adminUserService.getUserActivity(userId, page, size));
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users/{userId}/transactions?page=0&size=20
     * Returns paginated transactions for the given user, newest first.
     */
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<Page<AdminUserTransactionResponse>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(adminUserService.getUserTransactions(userId, page, size));
    }

    // ── Wallets ───────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users/{userId}/wallets
     * Returns all wallets owned by the given user.
     */
    @GetMapping("/{userId}/wallets")
    public ResponseEntity<List<AdminUserWalletResponse>> getUserWallets(
            @PathVariable Long userId) {

        return ResponseEntity.ok(adminUserService.getUserWallets(userId));
    }

    // ── Bulk operations ───────────────────────────────────────────────────────

    /**
     * POST /api/v1/admin/users/bulk-ban
     * Body: { "userIds": [1, 2, 3], "reason": "Policy violation" }
     */
    @PostMapping("/bulk-ban")
    public ResponseEntity<Void> bulkBan(@Valid @RequestBody AdminBulkBanRequest request) {
        adminUserService.banUsersBulk(request.getUserIds(), request.getReason());
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/admin/users/bulk-email
     * Body: { "userIds": [1, 2, 3], "subject": "Important notice", "content": "..." }
     */
    @PostMapping("/bulk-email")
    public ResponseEntity<Void> bulkEmail(@Valid @RequestBody AdminBulkEmailRequest request) {
        adminUserService.emailUsersBulk(
                request.getUserIds(), request.getSubject(), request.getContent());
        return ResponseEntity.ok().build();
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users/export
     * Downloads all users as a UTF-8 CSV file.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csv = adminUserService.exportUsersToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"users_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(csv.length)
                .body(csv);
    }
}
