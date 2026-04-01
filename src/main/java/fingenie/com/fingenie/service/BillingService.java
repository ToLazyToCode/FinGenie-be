package fingenie.com.fingenie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.config.BillingProperties;
import fingenie.com.fingenie.dto.BillingCheckoutRequest;
import fingenie.com.fingenie.dto.BillingCheckoutResponse;
import fingenie.com.fingenie.dto.BillingOrderResponse;
import fingenie.com.fingenie.dto.BillingPlanResponse;
import fingenie.com.fingenie.dto.BillingReturnResponse;
import fingenie.com.fingenie.dto.BillingWebhookAckResponse;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.PaymentEvent;
import fingenie.com.fingenie.entity.PaymentEventType;
import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrder;
import fingenie.com.fingenie.entity.PaymentOrderStatus;
import fingenie.com.fingenie.entity.SubscriptionPlan;
import fingenie.com.fingenie.entity.UserSubscription;
import fingenie.com.fingenie.entity.UserSubscriptionStatus;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.PaymentEventRepository;
import fingenie.com.fingenie.repository.PaymentOrderRepository;
import fingenie.com.fingenie.repository.SubscriptionPlanRepository;
import fingenie.com.fingenie.repository.UserSubscriptionRepository;
import fingenie.com.fingenie.utils.PaymentSignatureUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private static final String RSP_SUCCESS = "RspCode=00&Message=Confirm Success";
    private static final String RSP_ALREADY = "RspCode=02&Message=Order already confirmed";
    private static final String RSP_NOT_FOUND = "RspCode=01&Message=Order not found";
    private static final String RSP_INVALID_AMOUNT = "RspCode=04&Message=Invalid amount";
    private static final String RSP_INVALID_SIGNATURE = "RspCode=97&Message=Invalid signature";

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final List<BillingGatewayService> gatewayServices;
    private final BillingProperties billingProperties;

    @Transactional
    public List<BillingPlanResponse> getPlans() {
        ensureDefaultPlans();
        return subscriptionPlanRepository.findByIsActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BillingCheckoutResponse createCheckout(
            Long accountId,
            BillingCheckoutRequest request,
            String clientIp,
            String publicBaseUrl
    ) {
        ensureDefaultPlans();
        if (request == null || isBlank(request.getPlanCode())) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "reason", "invalidCheckoutRequest"
            ));
        }

        PaymentGateway requestedGateway = request.getGateway() == null
                ? PaymentGateway.PAYOS
                : request.getGateway();

        if (requestedGateway != PaymentGateway.PAYOS) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "unsupportedForNewCheckout",
                    "allowedGateway", PaymentGateway.PAYOS.name(),
                    "requestedGateway", requestedGateway.name()
            ));
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCodeAndIsActiveTrue(request.getPlanCode().trim())
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "planCode", "notFoundOrInactive",
                        "value", request.getPlanCode()
                )));

        BillingGatewayService gatewayService = resolveGatewayService(PaymentGateway.PAYOS);
        PaymentOrder order = PaymentOrder.builder()
                .orderCode(generateOrderCode())
                .accountId(accountId)
                .plan(plan)
                .gateway(PaymentGateway.PAYOS)
                .amount(plan.getAmount())
                .status(PaymentOrderStatus.PENDING)
                .expiresAt(Timestamp.from(Instant.now().plusSeconds(Math.max(60, billingProperties.getCheckoutExpireMinutes() * 60L))))
                .build();
        order = paymentOrderRepository.save(order);

        try {
            GatewayCheckoutResult gatewayResult = gatewayService.createCheckout(order, plan, clientIp, publicBaseUrl);
            order.setCheckoutUrl(gatewayResult.getCheckoutUrl());
            order.setGatewayOrderRef(gatewayResult.getGatewayOrderRef());
            order.setRawInitPayload(gatewayResult.getRawPayload());
            order.setStatus(PaymentOrderStatus.REDIRECTED);
            order = paymentOrderRepository.save(order);
            return toCheckoutResponse(order);
        } catch (Exception ex) {
            log.error("Checkout init failed gateway={} orderCode={}", PaymentGateway.PAYOS, order.getOrderCode(), ex);
            order.setStatus(PaymentOrderStatus.FAILED);
            order.setRawInitPayload("INIT_ERROR: " + ex.getMessage());
            paymentOrderRepository.save(order);
            throw ex;
        }
    }

    @Transactional
    public BillingOrderResponse getOrderForUser(Long accountId, String orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCodeAndAccountId(orderCode, accountId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "orderCode", "notFound",
                        "value", orderCode
                )));
        markExpiredIfNeeded(order);
        return toOrderResponse(order);
    }

    @Transactional
    public BillingOrderResponse getOrderByCode(String orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "orderCode", "notFound",
                        "value", orderCode
                )));
        markExpiredIfNeeded(order);
        return toOrderResponse(order);
    }

    @Transactional
    public BillingReturnResponse handlePayOSReturn(Map<String, String> params) {
        String orderCode = trimToNull(params.get("orderCode"));
        String eventRaw = toJsonSafely(params);
        recordReturnEvent(orderCode, PaymentGateway.PAYOS, eventRaw, params.get("signature"), false, "payos-return");
        PaymentOrderStatus status = resolveOrderStatus(orderCode);
        return BillingReturnResponse.builder()
                .orderCode(orderCode)
                .gateway(PaymentGateway.PAYOS)
                .status(status)
                .message("Return URL only. Final status depends on webhook.")
                .build();
    }

    @Transactional
    public BillingReturnResponse handleVNPayReturn(Map<String, String> params) {
        String orderCode = trimToNull(params.get("vnp_TxnRef"));
        boolean signatureValid = verifyVNPaySignature(params);
        String canonical = PaymentSignatureUtils.buildCanonicalString(new LinkedHashMap<>(params), true, true);
        String eventHash = buildEventHash(PaymentGateway.VNPAY, PaymentEventType.RETURN, canonical);
        PaymentEvent event = paymentEventRepository.findByEventHash(eventHash)
                .orElseGet(() -> {
                    PaymentOrder order = orderCode == null
                            ? null
                            : paymentOrderRepository.findByOrderCode(orderCode).orElse(null);
                    PaymentEvent created = createEvent(
                            order,
                            PaymentGateway.VNPAY,
                            PaymentEventType.RETURN,
                            eventHash,
                            trimToNull(params.get("vnp_TransactionNo")),
                            params.get("vnp_SecureHash"),
                            signatureValid,
                            canonical
                    );
                    return saveEventSafely(created);
                });

        String responseCode = trimToNull(params.get("vnp_ResponseCode"));
        String transactionStatus = trimToNull(params.get("vnp_TransactionStatus"));
        String gatewayTransactionRef = trimToNull(params.get("vnp_TransactionNo"));
        String message;

        if (!signatureValid) {
            markEventProcessed(event, "invalid_signature");
            message = "Return code=" + (responseCode == null ? "N/A" : responseCode) + ". Invalid VNPay signature.";
        } else {
            long amount = parseVNPayAmount(params.get("vnp_Amount"));
            boolean success = "00".equals(responseCode)
                    && (transactionStatus == null || "00".equals(transactionStatus));

            ProcessingOutcome outcome = applyGatewayPaymentResult(
                    orderCode,
                    PaymentGateway.VNPAY,
                    amount,
                    gatewayTransactionRef,
                    success,
                    event
            );

            message = switch (outcome) {
                case PAID_APPLIED, ALREADY_PAID ->
                        "Return code=" + (responseCode == null ? "N/A" : responseCode) + ". Payment confirmed.";
                case MARKED_FAILED ->
                        "Return code=" + (responseCode == null ? "N/A" : responseCode) + ". Payment was marked failed.";
                case INVALID_AMOUNT ->
                        "Return code=" + (responseCode == null ? "N/A" : responseCode) + ". Amount verification failed.";
                case ORDER_NOT_FOUND ->
                        "Return code=" + (responseCode == null ? "N/A" : responseCode) + ". Order not found.";
            };
        }

        PaymentOrderStatus status = resolveOrderStatus(orderCode);
        return BillingReturnResponse.builder()
                .orderCode(orderCode)
                .gateway(PaymentGateway.VNPAY)
                .status(status)
                .message(message)
                .build();
    }

    @Transactional
    public BillingWebhookAckResponse handlePayOSWebhook(String rawPayload, String signatureHeader) {
        if (isBlank(rawPayload)) {
            return BillingWebhookAckResponse.builder()
                    .success(false)
                    .code("400")
                    .message("Empty payload")
                    .build();
        }

        Map<String, Object> root = parseJsonObject(rawPayload);
        String signature = trimToNull(toStringValue(root.get("signature")));
        if (signature == null) {
            signature = trimToNull(signatureHeader);
        }
        Map<String, String> dataMap = normalizeStringMap(root.get("data"));
        String orderCode = trimToNull(dataMap.get("orderCode"));
        String eventHash = buildEventHash(PaymentGateway.PAYOS, PaymentEventType.WEBHOOK, rawPayload);
        Optional<PaymentEvent> existing = paymentEventRepository.findByEventHash(eventHash);
        if (existing.isPresent()) {
            return BillingWebhookAckResponse.builder()
                    .success(true)
                    .code("00")
                    .message("Duplicate webhook ignored")
                    .build();
        }

        boolean signatureValid = verifyPayOSSignature(dataMap, signature);
        PaymentEvent event = createEvent(
                null,
                PaymentGateway.PAYOS,
                PaymentEventType.WEBHOOK,
                eventHash,
                dataMap.get("reference"),
                signature,
                signatureValid,
                rawPayload
        );
        event = saveEventSafely(event);

        if (!signatureValid) {
            markEventProcessed(event, "invalid_signature");
            return BillingWebhookAckResponse.builder()
                    .success(false)
                    .code("97")
                    .message("Invalid signature")
                    .build();
        }

        long amount = parseAmount(dataMap.get("amount"));
        boolean successFlag = resolvePayOSSuccess(root, dataMap);
        String gatewayTransactionRef = trimToNull(dataMap.get("reference"));

        ProcessingOutcome outcome = applyGatewayPaymentResult(
                orderCode,
                PaymentGateway.PAYOS,
                amount,
                gatewayTransactionRef,
                successFlag,
                event
        );
        return BillingWebhookAckResponse.builder()
                .success(outcome == ProcessingOutcome.PAID_APPLIED || outcome == ProcessingOutcome.ALREADY_PAID)
                .code(outcome == ProcessingOutcome.INVALID_AMOUNT ? "04" : "00")
                .message(outcome.name())
                .build();
    }

    @Transactional
    public String handleVNPayIpn(Map<String, String> params) {
        String orderCode = trimToNull(params.get("vnp_TxnRef"));
        if (!verifyVNPaySignature(params)) {
            recordIpnEvent(orderCode, PaymentGateway.VNPAY, params, false, "invalid_signature");
            return RSP_INVALID_SIGNATURE;
        }

        String canonical = PaymentSignatureUtils.buildCanonicalString(new LinkedHashMap<>(params), true, true);
        String eventHash = buildEventHash(PaymentGateway.VNPAY, PaymentEventType.IPN, canonical);
        if (paymentEventRepository.findByEventHash(eventHash).isPresent()) {
            return RSP_ALREADY;
        }

        long amount = parseVNPayAmount(params.get("vnp_Amount"));
        String responseCode = trimToNull(params.get("vnp_ResponseCode"));
        String transactionStatus = trimToNull(params.get("vnp_TransactionStatus"));
        String gatewayTransactionRef = trimToNull(params.get("vnp_TransactionNo"));
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        PaymentEvent event = createEvent(
                null,
                PaymentGateway.VNPAY,
                PaymentEventType.IPN,
                eventHash,
                gatewayTransactionRef,
                params.get("vnp_SecureHash"),
                true,
                canonical
        );
        event = saveEventSafely(event);

        ProcessingOutcome outcome = applyGatewayPaymentResult(
                orderCode,
                PaymentGateway.VNPAY,
                amount,
                gatewayTransactionRef,
                success,
                event
        );
        return switch (outcome) {
            case PAID_APPLIED, ALREADY_PAID -> RSP_SUCCESS;
            case ORDER_NOT_FOUND -> RSP_NOT_FOUND;
            case INVALID_AMOUNT -> RSP_INVALID_AMOUNT;
            default -> "RspCode=00&Message=Recorded";
        };
    }

    private ProcessingOutcome applyGatewayPaymentResult(
            String orderCode,
            PaymentGateway gateway,
            long amount,
            String gatewayTransactionRef,
            boolean success,
            PaymentEvent event
    ) {
        if (isBlank(orderCode)) {
            markEventProcessed(event, "missing_order_code");
            return ProcessingOutcome.ORDER_NOT_FOUND;
        }

        Optional<PaymentOrder> lockedOrderOpt = paymentOrderRepository.findByOrderCodeForUpdate(orderCode);
        if (lockedOrderOpt.isEmpty()) {
            markEventProcessed(event, "order_not_found");
            return ProcessingOutcome.ORDER_NOT_FOUND;
        }

        PaymentOrder order = lockedOrderOpt.get();
        event.setPaymentOrder(order);

        if (!order.getAmount().equals(amount)) {
            order.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);
            markEventProcessed(event, "amount_mismatch");
            return ProcessingOutcome.INVALID_AMOUNT;
        }

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            markEventProcessed(event, "already_paid");
            return ProcessingOutcome.ALREADY_PAID;
        }

        if (!success) {
            order.setStatus(PaymentOrderStatus.FAILED);
            order.setGatewayTransactionRef(gatewayTransactionRef);
            paymentOrderRepository.save(order);
            markEventProcessed(event, "gateway_marked_failed");
            return ProcessingOutcome.MARKED_FAILED;
        }

        order.setStatus(PaymentOrderStatus.PAID);
        order.setGateway(gateway);
        order.setGatewayTransactionRef(gatewayTransactionRef);
        order.setPaidAt(Timestamp.from(Instant.now()));
        paymentOrderRepository.save(order);

        activateSubscription(order);
        markEventProcessed(event, "paid_and_subscription_activated");
        return ProcessingOutcome.PAID_APPLIED;
    }

    private void activateSubscription(PaymentOrder order) {
        Account account = accountRepository.findById(order.getAccountId())
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "accountId", "notFound",
                        "value", order.getAccountId()
                )));

        UserSubscription subscription = userSubscriptionRepository.findByAccountIdForUpdate(order.getAccountId())
                .orElse(null);

        Timestamp now = Timestamp.from(Instant.now());
        Timestamp startsAt = now;
        if (subscription != null
                && subscription.getStatus() == UserSubscriptionStatus.ACTIVE
                && subscription.getEndsAt() != null
                && subscription.getEndsAt().after(now)) {
            startsAt = subscription.getEndsAt();
        }
        Timestamp endsAt = Timestamp.valueOf(
                startsAt.toLocalDateTime().plusDays(Math.max(1, order.getPlan().getDurationDays()))
        );

        if (subscription == null) {
            subscription = UserSubscription.builder()
                    .accountId(order.getAccountId())
                    .plan(order.getPlan())
                    .status(UserSubscriptionStatus.ACTIVE)
                    .startsAt(now)
                    .endsAt(endsAt)
                    .lastPaymentOrderCode(order.getOrderCode())
                    .build();
        } else {
            subscription.setPlan(order.getPlan());
            subscription.setStatus(UserSubscriptionStatus.ACTIVE);
            subscription.setStartsAt(now);
            subscription.setEndsAt(endsAt);
            subscription.setLastPaymentOrderCode(order.getOrderCode());
        }
        userSubscriptionRepository.save(subscription);

        if (!account.isPremium()) {
            account.setPremium(true);
            accountRepository.save(account);
        }
    }

    private void ensureDefaultPlans() {
        seedPlan("PREMIUM_MONTHLY_BASIC", "Premium Basic", "30 ngay premium co ban", 79000L, 30, 1);
        seedPlan("PREMIUM_MONTHLY_PRO", "Premium Pro", "30 ngay premium day du", 129000L, 30, 2);
        seedPlan("PREMIUM_YEARLY_PRO", "Premium Pro Yearly", "365 ngay premium tiet kiem hon", 1199000L, 365, 3);
    }

    private void seedPlan(
            String planCode,
            String title,
            String description,
            long amount,
            int durationDays,
            int sortOrder
    ) {
        if (subscriptionPlanRepository.findByPlanCode(planCode).isPresent()) {
            return;
        }
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planCode(planCode)
                .title(title)
                .description(description)
                .amount(amount)
                .currency("VND")
                .durationDays(durationDays)
                .sortOrder(sortOrder)
                .build();
        try {
            subscriptionPlanRepository.save(plan);
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent startup seeding.
        }
    }

    private String generateOrderCode() {
        for (int i = 0; i < 6; i++) {
            long candidate = System.currentTimeMillis() * 10 + ThreadLocalRandom.current().nextInt(10);
            String code = String.valueOf(candidate);
            if (paymentOrderRepository.findByOrderCode(code).isEmpty()) {
                return code;
            }
        }
        return String.valueOf(System.currentTimeMillis());
    }

    private BillingGatewayService resolveGatewayService(PaymentGateway gateway) {
        return gatewayServices.stream()
                .filter(service -> service.supportedGateway() == gateway)
                .findFirst()
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "gateway", "unsupported",
                        "value", gateway.name()
                )));
    }

    private BillingPlanResponse toPlanResponse(SubscriptionPlan plan) {
        return BillingPlanResponse.builder()
                .planCode(plan.getPlanCode())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .durationDays(plan.getDurationDays())
                .build();
    }

    private BillingCheckoutResponse toCheckoutResponse(PaymentOrder order) {
        return BillingCheckoutResponse.builder()
                .orderCode(order.getOrderCode())
                .planCode(order.getPlan().getPlanCode())
                .gateway(order.getGateway())
                .amount(order.getAmount())
                .status(order.getStatus())
                .checkoutUrl(order.getCheckoutUrl())
                .expiresAt(order.getExpiresAt())
                .build();
    }

    private BillingOrderResponse toOrderResponse(PaymentOrder order) {
        return BillingOrderResponse.builder()
                .orderCode(order.getOrderCode())
                .planCode(order.getPlan().getPlanCode())
                .planTitle(order.getPlan().getTitle())
                .gateway(order.getGateway())
                .amount(order.getAmount())
                .status(order.getStatus())
                .checkoutUrl(order.getCheckoutUrl())
                .gatewayOrderRef(order.getGatewayOrderRef())
                .gatewayTransactionRef(order.getGatewayTransactionRef())
                .paidAt(order.getPaidAt())
                .expiresAt(order.getExpiresAt())
                .build();
    }

    private void markEventProcessed(PaymentEvent event, String result) {
        event.setProcessed(true);
        event.setProcessingResult(result);
        event.setProcessedAt(Timestamp.from(Instant.now()));
        paymentEventRepository.save(event);
    }

    private PaymentEvent createEvent(
            PaymentOrder order,
            PaymentGateway gateway,
            PaymentEventType eventType,
            String eventHash,
            String gatewayEventRef,
            String signature,
            boolean signatureValid,
            String rawPayload
    ) {
        return PaymentEvent.builder()
                .paymentOrder(order)
                .gateway(gateway)
                .eventType(eventType)
                .eventHash(eventHash)
                .gatewayEventRef(gatewayEventRef)
                .signature(signature)
                .signatureValid(signatureValid)
                .rawPayload(rawPayload)
                .processed(false)
                .build();
    }

    private PaymentEvent saveEventSafely(PaymentEvent event) {
        try {
            return paymentEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            return paymentEventRepository.findByEventHash(event.getEventHash())
                    .orElseThrow(() -> ex);
        }
    }

    private void recordReturnEvent(
            String orderCode,
            PaymentGateway gateway,
            String rawPayload,
            String signature,
            boolean signatureValid,
            String result
    ) {
        String eventHash = buildEventHash(gateway, PaymentEventType.RETURN, rawPayload);
        if (paymentEventRepository.findByEventHash(eventHash).isPresent()) {
            return;
        }
        PaymentOrder order = orderCode == null ? null : paymentOrderRepository.findByOrderCode(orderCode).orElse(null);
        PaymentEvent event = createEvent(
                order,
                gateway,
                PaymentEventType.RETURN,
                eventHash,
                null,
                signature,
                signatureValid,
                rawPayload
        );
        event.setProcessed(true);
        event.setProcessingResult(result);
        event.setProcessedAt(Timestamp.from(Instant.now()));
        saveEventSafely(event);
    }

    private void recordIpnEvent(
            String orderCode,
            PaymentGateway gateway,
            Map<String, String> params,
            boolean signatureValid,
            String result
    ) {
        String raw = PaymentSignatureUtils.buildCanonicalString(new LinkedHashMap<>(params), true, true);
        String eventHash = buildEventHash(gateway, PaymentEventType.IPN, raw);
        if (paymentEventRepository.findByEventHash(eventHash).isPresent()) {
            return;
        }
        PaymentOrder order = orderCode == null ? null : paymentOrderRepository.findByOrderCode(orderCode).orElse(null);
        PaymentEvent event = createEvent(
                order,
                gateway,
                PaymentEventType.IPN,
                eventHash,
                null,
                params.get("vnp_SecureHash"),
                signatureValid,
                raw
        );
        event.setProcessed(true);
        event.setProcessingResult(result);
        event.setProcessedAt(Timestamp.from(Instant.now()));
        saveEventSafely(event);
    }

    private String buildEventHash(PaymentGateway gateway, PaymentEventType eventType, String rawPayload) {
        return PaymentSignatureUtils.sha256Hex(gateway.name() + "|" + eventType.name() + "|" + rawPayload);
    }

    private PaymentOrderStatus resolveOrderStatus(String orderCode) {
        if (orderCode == null) {
            return PaymentOrderStatus.PENDING;
        }
        return paymentOrderRepository.findByOrderCode(orderCode)
                .map(order -> {
                    markExpiredIfNeeded(order);
                    return order.getStatus();
                })
                .orElse(PaymentOrderStatus.PENDING);
    }

    private void markExpiredIfNeeded(PaymentOrder order) {
        if (order == null) {
            return;
        }
        if (order.getStatus() != PaymentOrderStatus.PENDING
                && order.getStatus() != PaymentOrderStatus.REDIRECTED) {
            return;
        }
        Timestamp expiresAt = order.getExpiresAt();
        if (expiresAt == null) {
            return;
        }
        if (expiresAt.before(Timestamp.from(Instant.now()))) {
            order.setStatus(PaymentOrderStatus.EXPIRED);
            paymentOrderRepository.save(order);
        }
    }

    private boolean resolvePayOSSuccess(Map<String, Object> root, Map<String, String> dataMap) {
        Object successObj = root.get("success");
        if (successObj != null) {
            if (successObj instanceof Boolean boolVal) {
                return boolVal;
            }
            if (successObj instanceof Number numVal) {
                return numVal.intValue() == 1;
            }
            String stringValue = String.valueOf(successObj).trim();
            if ("true".equalsIgnoreCase(stringValue) || "1".equals(stringValue)) {
                return true;
            }
            if ("false".equalsIgnoreCase(stringValue) || "0".equals(stringValue)) {
                return false;
            }
        }

        String code = trimToNull(toStringValue(root.get("code")));
        if ("00".equals(code)) {
            return true;
        }
        String dataCode = trimToNull(dataMap.get("code"));
        if ("00".equals(dataCode)) {
            return true;
        }
        return false;
    }

    private boolean verifyPayOSSignature(Map<String, String> data, String signature) {
        if (isBlank(signature) || data == null || data.isEmpty()) {
            return false;
        }
        String checksumKey = billingProperties.getPayos().getChecksumKey();
        if (isBlank(checksumKey)) {
            return false;
        }
        String canonical = PaymentSignatureUtils.buildCanonicalString(data, false, false);
        String expected = PaymentSignatureUtils.hmacSha256Hex(canonical, checksumKey);
        return expected.equalsIgnoreCase(signature);
    }

    private boolean verifyVNPaySignature(Map<String, String> params) {
        String secureHash = trimToNull(params.get("vnp_SecureHash"));
        if (secureHash == null) {
            return false;
        }
        String hashSecret = billingProperties.getVnpay().getHashSecret();
        if (isBlank(hashSecret)) {
            return false;
        }

        Map<String, String> filtered = new HashMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");
        filtered.values().removeIf(value -> value == null || value.isBlank());

        String canonical = PaymentSignatureUtils.buildCanonicalString(filtered, false, true);
        String expected = PaymentSignatureUtils.hmacSha512Hex(canonical, hashSecret);
        return expected.equalsIgnoreCase(secureHash);
    }

    private long parseAmount(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private long parseVNPayAmount(String value) {
        long raw = parseAmount(value);
        if (raw <= 0) {
            return 0L;
        }
        return raw / 100L;
    }

    private Map<String, Object> parseJsonObject(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "reason", "invalidJsonPayload"
            ));
        }
    }

    private Map<String, String> normalizeStringMap(Object value) {
        if (!(value instanceof Map<?, ?> input)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String toJsonSafely(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum ProcessingOutcome {
        PAID_APPLIED,
        ALREADY_PAID,
        ORDER_NOT_FOUND,
        INVALID_AMOUNT,
        MARKED_FAILED
    }
}
