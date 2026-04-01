package fingenie.com.fingenie.admin.service;

import fingenie.com.fingenie.admin.dto.*;
import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrder;
import fingenie.com.fingenie.entity.PaymentOrderStatus;
import fingenie.com.fingenie.entity.Refund;
import fingenie.com.fingenie.entity.RefundStatus;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.PaymentOrderRepository;
import fingenie.com.fingenie.repository.RefundRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for the admin financial management section.
 *
 * <h3>Data sources</h3>
 * <ul>
 *   <li><b>Transactions / Financial summary / Gateway analytics</b> –
 *       sourced from {@link PaymentOrder} (real subscription payments via gateways).</li>
 *   <li><b>Refunds</b> –
 *       sourced from the {@link Refund} entity (admin-managed refund workflow).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFinancialService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final RefundRepository       refundRepository;
    private final AccountRepository      accountRepository;

    // ── Financial Summary ─────────────────────────────────────────────────────

    /**
     * Returns aggregated KPI figures for the financial summary cards.
     */
    @Transactional(readOnly = true)
    public AdminFinancialSummaryResponse getSummary() {
        Timestamp since24h = toTimestamp(LocalDateTime.now().minusHours(24));
        Timestamp since7d  = toTimestamp(LocalDateTime.now().minusDays(7));
        Timestamp since30d = toTimestamp(LocalDateTime.now().minusDays(30));

        BigDecimal volume24h = toDecimal(
                paymentOrderRepository.sumAmountByStatusSince(PaymentOrderStatus.PAID, since24h));
        BigDecimal volume7d = toDecimal(
                paymentOrderRepository.sumAmountByStatusSince(PaymentOrderStatus.PAID, since7d));
        BigDecimal volume30d = toDecimal(
                paymentOrderRepository.sumAmountByStatusSince(PaymentOrderStatus.PAID, since30d));
        BigDecimal totalRevenue = toDecimal(
                paymentOrderRepository.sumAmountByStatus(PaymentOrderStatus.PAID));

        Double avgRaw = paymentOrderRepository.avgAmountByStatus(PaymentOrderStatus.PAID);
        BigDecimal avgAmount = avgRaw != null
                ? BigDecimal.valueOf(avgRaw).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long total           = paymentOrderRepository.count();
        long paid            = paymentOrderRepository.countByStatus(PaymentOrderStatus.PAID);
        long approvedRefunds = refundRepository.countByStatus(RefundStatus.APPROVED);

        double successRate = total > 0 ? round((paid * 100.0) / total) : 0.0;
        double refundRate  = total > 0 ? round((approvedRefunds * 100.0) / total) : 0.0;

        return AdminFinancialSummaryResponse.builder()
                .volume24h(volume24h)
                .volume7d(volume7d)
                .volume30d(volume30d)
                .totalRevenue(totalRevenue)
                .averageTransactionAmount(avgAmount)
                .successRate(successRate)
                .refundRate(refundRate)
                .totalTransactions(total)
                .build();
    }

    // ── Transaction List (PaymentOrders) ──────────────────────────────────────

    /**
     * Paginated admin view of payment orders with optional filters.
     *
     * @param page     zero-based page index
     * @param size     page size (capped at 200)
     * @param status   filter by {@link PaymentOrderStatus} name (nullable)
     * @param gateway  filter by {@link PaymentGateway} name (nullable)
     * @param dateFrom inclusive lower bound on createdAt (nullable)
     * @param dateTo   inclusive upper bound on createdAt (nullable)
     */
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getTransactions(
            int page, int size,
            String status, String gateway,
            LocalDate dateFrom, LocalDate dateTo) {

        PaymentOrderStatus statusEnum = parseEnum(PaymentOrderStatus.class, status);
        PaymentGateway     gatewayEnum = parseEnum(PaymentGateway.class, gateway);
        Timestamp          from = dateFrom != null ? toTimestamp(dateFrom.atStartOfDay()) : null;
        Timestamp          to   = dateTo   != null ? toTimestamp(dateTo.atTime(23, 59, 59)) : null;

        int cappedSize = Math.min(size, 200);
        Page<PaymentOrder> orderPage = paymentOrderRepository.findForAdmin(
                statusEnum, gatewayEnum, from, to,
                PageRequest.of(page, cappedSize));

        // Batch-load accounts to avoid N+1
        Set<Long> accountIds = orderPage.getContent().stream()
                .map(PaymentOrder::getAccountId)
                .collect(Collectors.toSet());
        Map<Long, String> emailByAccountId = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(
                        a -> a.getId(),
                        a -> a.getEmail()
                ));

        List<AdminTransactionResponse> content = orderPage.getContent().stream()
                .map(o -> mapToTransactionResponse(o, emailByAccountId))
                .toList();

        return new PageImpl<>(content, orderPage.getPageable(), orderPage.getTotalElements());
    }

    /**
     * Returns a single payment order by its ID.
     */
    @Transactional(readOnly = true)
    public AdminTransactionResponse getTransactionDetail(Long transactionId) {
        PaymentOrder order = paymentOrderRepository.findByIdWithPlan(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PaymentOrder not found: " + transactionId));

        String userEmail = accountRepository.findById(order.getAccountId())
                .map(a -> a.getEmail())
                .orElse("unknown");

        return mapToTransactionResponse(order, Map.of(order.getAccountId(), userEmail));
    }

    // ── Refund Workflow ───────────────────────────────────────────────────────

    /**
     * Returns paginated refunds in {@code PENDING} status, newest first.
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundResponse> getPendingRefunds(int page, int size) {
        Page<Refund> refundPage = refundRepository.findByStatus(
                RefundStatus.PENDING,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")));
        return refundPage.map(this::mapToRefundResponse);
    }

    /**
     * Returns all refunds with an optional status filter, newest first.
     *
     * @param status nullable – omit to return all statuses
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundResponse> getAllRefunds(int page, int size, String status) {
        RefundStatus statusEnum = parseEnum(RefundStatus.class, status);
        Page<Refund> refundPage = statusEnum != null
                ? refundRepository.findByStatus(
                        statusEnum,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")))
                : refundRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")));
        return refundPage.map(this::mapToRefundResponse);
    }

    /**
     * Creates a new {@code PENDING} refund for the given payment order.
     *
     * @param transactionId PaymentOrder ID
     * @param amount        refund amount; {@code null} means full refund
     * @param reason        mandatory reason string
     */
    @Transactional
    public AdminRefundResponse initiateRefund(Long transactionId, BigDecimal amount, String reason) {
        PaymentOrder order = paymentOrderRepository.findByIdWithPlan(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PaymentOrder not found: " + transactionId));

        if (order.getStatus() != PaymentOrderStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot refund an order that is not in PAID status. Current status: "
                            + order.getStatus());
        }

        BigDecimal originalAmount = BigDecimal.valueOf(order.getAmount());
        BigDecimal refundAmount   = (amount != null) ? amount : originalAmount;

        if (refundAmount.compareTo(originalAmount) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount (" + refundAmount + ") exceeds original order amount ("
                            + originalAmount + ")");
        }

        Refund refund = Refund.builder()
                .paymentOrderId(order.getId())
                .originalAmount(originalAmount)
                .refundAmount(refundAmount)
                .reason(reason)
                .status(RefundStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        Refund saved = refundRepository.save(refund);
        log.info("[AdminFinancial] Refund initiated: refundId={} orderId={} amount={}",
                saved.getId(), transactionId, refundAmount);
        return mapToRefundResponse(saved);
    }

    /**
     * Approves a pending refund.
     *
     * @param refundId ID of the refund to approve
     * @param notes    optional admin notes
     */
    @Transactional
    public AdminRefundResponse approveRefund(Long refundId, String notes) {
        Refund refund = getRefundOrThrow(refundId);
        requirePending(refund);

        refund.setStatus(RefundStatus.APPROVED);
        refund.setProcessedAt(LocalDateTime.now());
        refund.setProcessedBy(currentAdminEmail());
        if (notes != null && !notes.isBlank()) {
            refund.setNotes(notes);
        }

        Refund saved = refundRepository.save(refund);
        log.info("[AdminFinancial] Refund approved: refundId={} by={}", refundId, saved.getProcessedBy());
        return mapToRefundResponse(saved);
    }

    /**
     * Rejects a pending refund.
     *
     * @param refundId ID of the refund to reject
     * @param reason   mandatory rejection reason
     */
    @Transactional
    public AdminRefundResponse rejectRefund(Long refundId, String reason) {
        Refund refund = getRefundOrThrow(refundId);
        requirePending(refund);

        refund.setStatus(RefundStatus.REJECTED);
        refund.setProcessedAt(LocalDateTime.now());
        refund.setProcessedBy(currentAdminEmail());
        refund.setNotes(reason);

        Refund saved = refundRepository.save(refund);
        log.info("[AdminFinancial] Refund rejected: refundId={} by={}", refundId, saved.getProcessedBy());
        return mapToRefundResponse(saved);
    }

    // ── Gateway Analytics ─────────────────────────────────────────────────────

    /**
     * Returns operational status and aggregate stats for every registered gateway.
     */
    @Transactional(readOnly = true)
    public List<AdminPaymentGatewayResponse> getPaymentGatewayStatus() {
        LocalDateTime checkTime = LocalDateTime.now();
        List<AdminPaymentGatewayResponse> result = new ArrayList<>();
        for (PaymentGateway gw : PaymentGateway.values()) {
            result.add(buildGatewayResponse(gw, null, null, checkTime));
        }
        return result;
    }

    /**
     * Returns gateway analytics filtered by an optional date range.
     *
     * @param gatewayName gateway name string (e.g. {@code "PAYOS"})
     * @param dateFrom    nullable lower bound
     * @param dateTo      nullable upper bound
     */
    @Transactional(readOnly = true)
    public AdminPaymentGatewayResponse getGatewayAnalytics(
            String gatewayName, LocalDate dateFrom, LocalDate dateTo) {

        PaymentGateway gw = parseEnum(PaymentGateway.class, gatewayName);
        if (gw == null) {
            throw new EntityNotFoundException("Unknown gateway: " + gatewayName);
        }

        Timestamp from = dateFrom != null ? toTimestamp(dateFrom.atStartOfDay()) : null;
        Timestamp to   = dateTo   != null ? toTimestamp(dateTo.atTime(23, 59, 59)) : null;

        return buildGatewayResponse(gw, from, to, LocalDateTime.now());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AdminPaymentGatewayResponse buildGatewayResponse(
            PaymentGateway gw,
            Timestamp dateFrom, Timestamp dateTo,
            LocalDateTime checkTime) {

        long total;
        long paid;
        long failed;
        BigDecimal volume;

        if (dateFrom == null && dateTo == null) {
            // All-time
            total  = paymentOrderRepository.countByGateway(gw);
            paid   = paymentOrderRepository.countByGatewayAndStatus(gw, PaymentOrderStatus.PAID);
            failed = paymentOrderRepository.countByGatewayAndStatus(gw, PaymentOrderStatus.FAILED);
            volume = toDecimal(paymentOrderRepository.sumAmountByGatewayAndStatus(gw, PaymentOrderStatus.PAID));
        } else {
            // Date-filtered
            total  = coalesce(paymentOrderRepository.countByGatewayAndDateRange(gw, dateFrom, dateTo));
            paid   = coalesce(paymentOrderRepository.countByGatewayAndStatusAndDateRange(
                    gw, PaymentOrderStatus.PAID, dateFrom, dateTo));
            failed = coalesce(paymentOrderRepository.countByGatewayAndStatusAndDateRange(
                    gw, PaymentOrderStatus.FAILED, dateFrom, dateTo));
            volume = toDecimal(paymentOrderRepository.sumAmountByGatewayAndStatusAndDateRange(
                    gw, PaymentOrderStatus.PAID, dateFrom, dateTo));
        }

        double successRate = total > 0 ? round((paid * 100.0) / total) : 0.0;

        String status;
        if (total == 0) {
            status = "DOWN";
        } else if (successRate < 80.0) {
            status = "DEGRADED";
        } else {
            status = "UP";
        }

        return AdminPaymentGatewayResponse.builder()
                .gatewayName(gw.name())
                .status(status)
                .totalTransactions(total)
                .successfulTransactions(paid)
                .failedTransactions(failed)
                .successRate(successRate)
                .totalVolume(volume)
                .averageResponseTime(0L)          // latency tracking not yet instrumented
                .lastHealthCheckAt(checkTime)
                .build();
    }

    private AdminTransactionResponse mapToTransactionResponse(
            PaymentOrder o, Map<Long, String> emailMap) {
        String userEmail = emailMap.getOrDefault(o.getAccountId(), "unknown");
        String planTitle = (o.getPlan() != null) ? o.getPlan().getTitle() : null;

        LocalDateTime ts = (o.getPaidAt() != null)
                ? o.getPaidAt().toLocalDateTime()
                : (o.getCreatedAt() != null ? o.getCreatedAt().toLocalDateTime() : null);

        LocalDateTime createdAt = o.getCreatedAt() != null
                ? o.getCreatedAt().toLocalDateTime() : null;

        return AdminTransactionResponse.builder()
                .id(o.getId())
                .userId(o.getAccountId())
                .userEmail(userEmail)
                .amount(BigDecimal.valueOf(o.getAmount()))
                .type("SUBSCRIPTION")
                .category(planTitle)
                .status(o.getStatus() != null ? o.getStatus().name() : null)
                .gateway(o.getGateway() != null ? o.getGateway().name() : null)
                .walletName(null)
                .description(o.getOrderCode())
                .timestamp(ts)
                .createdAt(createdAt)
                .build();
    }

    private AdminRefundResponse mapToRefundResponse(Refund r) {
        return AdminRefundResponse.builder()
                .id(r.getId())
                .transactionId(r.getPaymentOrderId())
                .originalAmount(r.getOriginalAmount())
                .refundAmount(r.getRefundAmount())
                .reason(r.getReason())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .requestedAt(r.getRequestedAt())
                .processedAt(r.getProcessedAt())
                .processedBy(r.getProcessedBy())
                .notes(r.getNotes())
                .build();
    }

    private Refund getRefundOrThrow(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Refund not found: " + refundId));
    }

    private void requirePending(Refund refund) {
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException(
                    "Refund " + refund.getId() + " is not in PENDING status. Current: "
                            + refund.getStatus());
        }
    }

    /** Resolves the authenticated admin's email from the SecurityContext. */
    private String currentAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "unknown";
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(clazz, value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("[AdminFinancial] Unknown enum value '{}' for {}", value, clazz.getSimpleName());
            return null;
        }
    }

    private static Timestamp toTimestamp(LocalDateTime ldt) {
        return Timestamp.valueOf(ldt);
    }

    private static BigDecimal toDecimal(Long val) {
        return BigDecimal.valueOf(val != null ? val : 0L);
    }

    private static long coalesce(Long val) {
        return val != null ? val : 0L;
    }

    private static double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
