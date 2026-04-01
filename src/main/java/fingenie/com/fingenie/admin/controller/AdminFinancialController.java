package fingenie.com.fingenie.admin.controller;

import fingenie.com.fingenie.admin.dto.*;
import fingenie.com.fingenie.admin.service.AdminFinancialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin endpoints for Financial Management.
 * All endpoints require the ADMIN role (also enforced at the filter-chain level).
 *
 * <pre>
 * GET  /api/v1/admin/financial/summary                             – KPI summary cards
 * GET  /api/v1/admin/transactions                                  – paginated payment orders
 * GET  /api/v1/admin/transactions/{transactionId}                  – single order detail
 * GET  /api/v1/admin/refunds/pending                               – pending refunds
 * GET  /api/v1/admin/refunds                                       – all refunds (filterable)
 * POST /api/v1/admin/transactions/{transactionId}/refund           – initiate refund
 * POST /api/v1/admin/refunds/{refundId}/approve                    – approve refund
 * POST /api/v1/admin/refunds/{refundId}/reject                     – reject refund
 * GET  /api/v1/admin/payment-gateways                             – gateway status list
 * GET  /api/v1/admin/payment-gateways/{gatewayName}/analytics      – gateway analytics
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFinancialController {

    private final AdminFinancialService adminFinancialService;

    // ── Summary ───────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/financial/summary
     *
     * Returns KPI summary cards: volume (24h/7d/30d), total revenue, success rate,
     * refund rate, average transaction amount, and total transaction count.
     */
    @GetMapping("${api-prefix}/admin/financial/summary")
    public ResponseEntity<AdminFinancialSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminFinancialService.getSummary());
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/transactions
     *
     * Paginated list of payment orders with optional filters.
     *
     * @param page      zero-based page index (default 0)
     * @param size      page size (default 20, capped at 200)
     * @param status    filter by PaymentOrderStatus name: PENDING | REDIRECTED | PAID | FAILED | EXPIRED | CANCELLED
     * @param gateway   filter by gateway: PAYOS | VNPAY
     * @param dateFrom  createdAt lower bound (inclusive, ISO date)
     * @param dateTo    createdAt upper bound (inclusive, ISO date)
     */
    @GetMapping("${api-prefix}/admin/transactions")
    public ResponseEntity<Page<AdminTransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String gateway,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        return ResponseEntity.ok(
                adminFinancialService.getTransactions(page, size, status, gateway, dateFrom, dateTo));
    }

    /**
     * GET /api/v1/admin/transactions/{transactionId}
     *
     * Returns the full detail of a single payment order.
     */
    @GetMapping("${api-prefix}/admin/transactions/{transactionId}")
    public ResponseEntity<AdminTransactionResponse> getTransactionDetail(
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(adminFinancialService.getTransactionDetail(transactionId));
    }

    // ── Refunds ───────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/refunds/pending
     *
     * Returns a paginated list of refunds awaiting admin review.
     */
    @GetMapping("${api-prefix}/admin/refunds/pending")
    public ResponseEntity<Page<AdminRefundResponse>> getPendingRefunds(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminFinancialService.getPendingRefunds(page, size));
    }

    /**
     * GET /api/v1/admin/refunds
     *
     * Returns all refunds, optionally filtered by status.
     *
     * @param status nullable – {@code PENDING | APPROVED | REJECTED}
     */
    @GetMapping("${api-prefix}/admin/refunds")
    public ResponseEntity<Page<AdminRefundResponse>> getAllRefunds(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(adminFinancialService.getAllRefunds(page, size, status));
    }

    /**
     * POST /api/v1/admin/transactions/{transactionId}/refund
     *
     * Initiates a refund for a PAID payment order.
     * Body: {@code { "refundAmount": 99000, "reason": "Customer request" }}
     * ({@code refundAmount} is optional – omit for a full refund.)
     */
    @PostMapping("${api-prefix}/admin/transactions/{transactionId}/refund")
    public ResponseEntity<AdminRefundResponse> initiateRefund(
            @PathVariable Long transactionId,
            @Valid @RequestBody AdminInitiateRefundRequest request) {
        return ResponseEntity.ok(adminFinancialService.initiateRefund(
                transactionId, request.getRefundAmount(), request.getReason()));
    }

    /**
     * POST /api/v1/admin/refunds/{refundId}/approve
     *
     * Approves a pending refund.
     * Body: {@code { "refundId": 1, "notes": "Verified, processing" }}
     */
    @PostMapping("${api-prefix}/admin/refunds/{refundId}/approve")
    public ResponseEntity<AdminRefundResponse> approveRefund(
            @PathVariable Long refundId,
            @RequestBody(required = false) AdminRefundApproveRequest request) {
        String notes = (request != null) ? request.getNotes() : null;
        return ResponseEntity.ok(adminFinancialService.approveRefund(refundId, notes));
    }

    /**
     * POST /api/v1/admin/refunds/{refundId}/reject
     *
     * Rejects a pending refund.
     * Body: {@code { "refundId": 1, "reason": "Duplicate request" }}
     */
    @PostMapping("${api-prefix}/admin/refunds/{refundId}/reject")
    public ResponseEntity<AdminRefundResponse> rejectRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody AdminRefundRejectRequest request) {
        return ResponseEntity.ok(adminFinancialService.rejectRefund(refundId, request.getReason()));
    }

    // ── Payment Gateways ──────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/payment-gateways
     *
     * Returns operational status and aggregate stats for all registered payment gateways
     * (PayOS, VNPay).
     */
    @GetMapping("${api-prefix}/admin/payment-gateways")
    public ResponseEntity<List<AdminPaymentGatewayResponse>> getPaymentGatewayStatus() {
        return ResponseEntity.ok(adminFinancialService.getPaymentGatewayStatus());
    }

    /**
     * GET /api/v1/admin/payment-gateways/{gatewayName}/analytics
     *
     * Returns analytics for a single gateway, optionally scoped to a date range.
     *
     * @param gatewayName gateway name (case-insensitive): {@code PAYOS} or {@code VNPAY}
     * @param dateFrom    optional lower bound (ISO date)
     * @param dateTo      optional upper bound (ISO date)
     */
    @GetMapping("${api-prefix}/admin/payment-gateways/{gatewayName}/analytics")
    public ResponseEntity<AdminPaymentGatewayResponse> getGatewayAnalytics(
            @PathVariable String gatewayName,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(
                adminFinancialService.getGatewayAnalytics(gatewayName, dateFrom, dateTo));
    }
}
