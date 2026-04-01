package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.config.BillingProperties;
import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrder;
import fingenie.com.fingenie.entity.SubscriptionPlan;
import fingenie.com.fingenie.utils.PaymentSignatureUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VNPayGatewayService implements BillingGatewayService {

    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BillingProperties billingProperties;

    @Override
    public PaymentGateway supportedGateway() {
        return PaymentGateway.VNPAY;
    }

    @Override
    public GatewayCheckoutResult createCheckout(
            PaymentOrder order,
            SubscriptionPlan plan,
            String clientIp,
            String publicBaseUrl
    ) {
        BillingProperties.VNPay config = billingProperties.getVnpay();
        if (!config.isEnabled()) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "vnpayDisabled"
            ));
        }
        if (isBlank(config.getTmnCode()) || isBlank(config.getHashSecret())) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "vnpayConfigMissing"
            ));
        }

        String effectivePublicBaseUrl = firstNonBlank(publicBaseUrl, billingProperties.getPublicBaseUrl());
        if (isBlank(effectivePublicBaseUrl)) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "vnpayPublicBaseUrlMissing"
            ));
        }

        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        String createDate = now.format(VNPAY_DATE_FORMAT);
        String expireDate = now.plusMinutes(Math.max(1, billingProperties.getCheckoutExpireMinutes()))
                .format(VNPAY_DATE_FORMAT);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", config.getVersion());
        params.put("vnp_Command", config.getCommand());
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(order.getAmount() * 100L));
        params.put("vnp_CurrCode", config.getCurrCode());
        params.put("vnp_TxnRef", order.getOrderCode());
        params.put("vnp_OrderInfo", safeOrderInfo(plan.getTitle()));
        params.put("vnp_OrderType", config.getOrderType());
        params.put("vnp_Locale", config.getLocale());
        params.put("vnp_ReturnUrl", concatUrl(effectivePublicBaseUrl, config.getReturnPath()));
        params.put("vnp_IpAddr", normalizeClientIp(clientIp));
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        String hashData = PaymentSignatureUtils.buildCanonicalString(params, false, true);
        String secureHash = PaymentSignatureUtils.hmacSha512Hex(hashData, config.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        String query = PaymentSignatureUtils.buildCanonicalString(params, true, true);
        String checkoutUrl = config.getPayUrl() + "?" + query;

        return GatewayCheckoutResult.builder()
                .checkoutUrl(checkoutUrl)
                .gatewayOrderRef(order.getOrderCode())
                .rawPayload(query)
                .build();
    }

    private String safeOrderInfo(String value) {
        if (value == null || value.isBlank()) {
            return "FinGenie Premium";
        }
        return value.length() > 200 ? value.substring(0, 200) : value;
    }

    private String normalizeClientIp(String value) {
        if (value == null || value.isBlank()) {
            return "127.0.0.1";
        }
        return value;
    }

    private String concatUrl(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }
}
