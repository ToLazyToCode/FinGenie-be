package fingenie.com.fingenie.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.config.BillingProperties;
import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrder;
import fingenie.com.fingenie.entity.SubscriptionPlan;
import fingenie.com.fingenie.utils.PaymentSignatureUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSGatewayService implements BillingGatewayService {

    private final BillingProperties billingProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplateBuilder restTemplateBuilder;

    @Override
    public PaymentGateway supportedGateway() {
        return PaymentGateway.PAYOS;
    }

    @Override
    public GatewayCheckoutResult createCheckout(
            PaymentOrder order,
            SubscriptionPlan plan,
            String clientIp,
            String publicBaseUrl
    ) {
        BillingProperties.PayOS config = billingProperties.getPayos();
        if (!config.isEnabled()) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "payosDisabled"
            ));
        }
        if (isBlank(config.getClientId()) || isBlank(config.getApiKey()) || isBlank(config.getChecksumKey())) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "payosConfigMissing"
            ));
        }
        String effectivePublicBaseUrl = firstNonBlank(publicBaseUrl, billingProperties.getPublicBaseUrl());
        if (isBlank(effectivePublicBaseUrl)) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "payosPublicBaseUrlMissing"
            ));
        }

        long orderCodeLong;
        try {
            orderCodeLong = Long.parseLong(order.getOrderCode());
        } catch (NumberFormatException ex) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "orderCode", order.getOrderCode(),
                    "reason", "payosOrderCodeMustBeNumeric"
            ));
        }

        String returnUrl = concatUrl(effectivePublicBaseUrl, config.getReturnPath());
        String cancelUrl = concatUrl(effectivePublicBaseUrl, config.getCancelPath());
        String description = safeDescription(plan.getTitle());

        Map<String, String> signPayload = new LinkedHashMap<>();
        signPayload.put("amount", String.valueOf(order.getAmount()));
        signPayload.put("cancelUrl", cancelUrl);
        signPayload.put("description", description);
        signPayload.put("orderCode", String.valueOf(orderCodeLong));
        signPayload.put("returnUrl", returnUrl);

        String signatureData = PaymentSignatureUtils.buildCanonicalString(signPayload, false, false);
        String signature = PaymentSignatureUtils.hmacSha256Hex(signatureData, config.getChecksumKey());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderCode", orderCodeLong);
        payload.put("amount", order.getAmount());
        payload.put("description", description);
        payload.put("returnUrl", returnUrl);
        payload.put("cancelUrl", cancelUrl);
        payload.put("signature", signature);
        payload.put("items", List.of(Map.of(
                "name", description,
                "quantity", 1,
                "price", order.getAmount()
        )));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", config.getClientId());
        headers.set("x-api-key", config.getApiKey());

        RestTemplate restTemplate = buildGatewayRestTemplate();
        String endpoint = concatUrl(config.getBaseUrl(), config.getCreatePath());
        ResponseEntity<String> response = restTemplate.postForEntity(
                endpoint,
                new HttpEntity<>(payload, headers),
                String.class
        );
        String responseBody = response.getBody() == null ? "" : response.getBody();
        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                    responseBody,
                    new TypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> data = toMap(responseMap.get("data"));
            String checkoutUrl = data == null ? null : toStringValue(data.get("checkoutUrl"));
            String paymentLinkId = data == null ? null : toStringValue(data.get("paymentLinkId"));
            if (isBlank(checkoutUrl)) {
                throw new SystemExceptions.ValidationException(Map.of(
                        "gateway", "payosInvalidResponse",
                        "body", responseBody
                ));
            }
            return GatewayCheckoutResult.builder()
                    .checkoutUrl(checkoutUrl)
                    .gatewayOrderRef(paymentLinkId)
                    .rawPayload(responseBody)
                    .build();
        } catch (Exception ex) {
            log.error("PayOS parse response failed orderCode={}", order.getOrderCode(), ex);
            throw new SystemExceptions.ValidationException(Map.of(
                    "gateway", "payosParseError",
                    "orderCode", order.getOrderCode()
            ));
        }
    }

    private RestTemplate buildGatewayRestTemplate() {
        int timeoutMs = Math.max(1000, billingProperties.getGatewayTimeoutMs());
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    private String safeDescription(String input) {
        String value = input == null ? "FinGenie Premium" : input.trim();
        if (value.isEmpty()) {
            value = "FinGenie Premium";
        }
        return value.length() > 25 ? value.substring(0, 25) : value;
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }
}
