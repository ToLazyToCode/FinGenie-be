package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.config.BillingProperties;
import fingenie.com.fingenie.dto.BillingCheckoutRequest;
import fingenie.com.fingenie.dto.BillingCheckoutResponse;
import fingenie.com.fingenie.dto.BillingOrderResponse;
import fingenie.com.fingenie.dto.BillingPlanResponse;
import fingenie.com.fingenie.dto.BillingReturnResponse;
import fingenie.com.fingenie.dto.BillingWebhookAckResponse;
import fingenie.com.fingenie.service.BillingService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"${api-prefix}/billing", "/api/billing"})
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Subscription plans and payment lifecycle")
public class BillingController {
    private static final String DEFAULT_MOBILE_RETURN_URL = "fingenie://redirect/payment";

    private final BillingService billingService;
    private final BillingProperties billingProperties;

    @GetMapping("/plans")
    @Operation(summary = "List active subscription plans")
    public ResponseEntity<List<BillingPlanResponse>> getPlans() {
        return ResponseEntity.ok(billingService.getPlans());
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create a payment order and PayOS checkout URL")
    public ResponseEntity<BillingCheckoutResponse> createCheckout(
            @Valid @RequestBody BillingCheckoutRequest request,
            HttpServletRequest servletRequest
    ) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        String clientIp = extractClientIp(servletRequest);
        String publicBaseUrl = resolvePublicBaseUrl(servletRequest);
        return ResponseEntity.ok(billingService.createCheckout(accountId, request, clientIp, publicBaseUrl));
    }

    @GetMapping("/orders/{orderCode}")
    @Operation(summary = "Get payment order status for current user")
    public ResponseEntity<BillingOrderResponse> getOrder(@PathVariable String orderCode) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(billingService.getOrderForUser(accountId, orderCode));
    }

    @GetMapping(value = "/return/payos", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "PayOS return URL (display only, no subscription activation)")
    public ResponseEntity<String> payosReturn(@RequestParam Map<String, String> params) {
        BillingReturnResponse result = billingService.handlePayOSReturn(params);
        return ResponseEntity.ok(buildReturnHtml(result));
    }

    @GetMapping(value = "/return/vnpay", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "VNPay return URL for legacy transactions (display only, no subscription activation)")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String, String> params) {
        BillingReturnResponse result = billingService.handleVNPayReturn(params);
        return ResponseEntity.ok(buildReturnHtml(result));
    }

    @PostMapping("/webhooks/payos")
    @Operation(summary = "PayOS webhook - source of truth for final payment confirmation")
    public ResponseEntity<BillingWebhookAckResponse> payosWebhook(
            @RequestBody(required = false) String rawPayload,
            @RequestHeader(value = "x-payos-signature", required = false) String signatureHeader
    ) {
        BillingWebhookAckResponse response = billingService.handlePayOSWebhook(rawPayload, signatureHeader);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/ipn/vnpay", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "VNPay IPN for legacy transactions - source of truth for final payment confirmation")
    public ResponseEntity<String> vnpayIpn(@RequestParam Map<String, String> params) {
        String ack = billingService.handleVNPayIpn(params);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .body(ack);
    }

    private String buildReturnHtml(BillingReturnResponse response) {
        String safeOrderCode = htmlEscape(nullToEmpty(response.getOrderCode()));
        String safeGateway = htmlEscape(response.getGateway() == null ? "UNKNOWN" : response.getGateway().name());
        String safeStatus = htmlEscape(response.getStatus() == null ? "PENDING" : response.getStatus().name());
        String safeMessage = htmlEscape(nullToEmpty(response.getMessage()));
        String appDeepLink = buildAppDeepLink(response);

        String redirectBlock = "";
        String buttonBlock = "";
        if (!isBlank(appDeepLink)) {
            String safeHref = htmlEscape(appDeepLink);
            buttonBlock = "<p><a href=\"" + safeHref + "\">Back to FinGenie</a></p>";
            if (billingProperties.isReturnPageAutoRedirectEnabled()) {
                int delayMs = Math.max(500, billingProperties.getReturnPageAutoRedirectDelayMs());
                redirectBlock = "<script>setTimeout(function(){window.location.href='"
                        + safeHref
                        + "';},"
                        + delayMs
                        + ");</script>";
            }
        }

        return "<!doctype html>"
                + "<html><head><meta charset=\"utf-8\" />"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />"
                + "<title>FinGenie Payment Return</title>"
                + "</head><body style=\"font-family:Arial,sans-serif;padding:24px;line-height:1.4;\">"
                + "<h2>FinGenie Payment Status</h2>"
                + "<p><strong>Order:</strong> " + safeOrderCode + "</p>"
                + "<p><strong>Gateway:</strong> " + safeGateway + "</p>"
                + "<p><strong>Current status:</strong> " + safeStatus + "</p>"
                + "<p><strong>Note:</strong> " + safeMessage + "</p>"
                + "<p>If confirmation is delayed, return to FinGenie and refresh once after a short wait.</p>"
                + buttonBlock
                + redirectBlock
                + "</body></html>";
    }

    private String buildAppDeepLink(BillingReturnResponse response) {
        String base = resolveMobileReturnUrl();
        if (isBlank(base)) {
            return null;
        }

        String separator = base.contains("?") ? "&" : "?";
        return base
                + separator
                + "orderCode="
                + urlEncode(nullToEmpty(response.getOrderCode()))
                + "&gateway="
                + urlEncode(response.getGateway() == null ? "UNKNOWN" : response.getGateway().name())
                + "&status="
                + urlEncode(response.getStatus() == null ? "PENDING" : response.getStatus().name());
    }

    private String resolveMobileReturnUrl() {
        String configured = billingProperties.getMobileReturnUrl();
        if (isBlank(configured)) {
            return DEFAULT_MOBILE_RETURN_URL;
        }

        String normalized = configured.trim();
        String lower = normalized.toLowerCase();
        if (lower.startsWith("fingenie://")) {
            return normalized;
        }

        // Reject dev-client / Metro return URLs so payment return always targets the app scheme.
        if (lower.startsWith("exp+") || lower.contains("expo-development-client") || lower.contains(":8081")) {
            return DEFAULT_MOBILE_RETURN_URL;
        }

        return DEFAULT_MOBILE_RETURN_URL;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!isBlank(forwardedFor)) {
            String[] split = forwardedFor.split(",");
            if (split.length > 0 && !isBlank(split[0])) {
                return split[0].trim();
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (!isBlank(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolvePublicBaseUrl(HttpServletRequest request) {
        String forwardedProto = firstForwardedValue(request.getHeader("X-Forwarded-Proto"));
        String forwardedHost = firstForwardedValue(request.getHeader("X-Forwarded-Host"));
        String forwardedPort = firstForwardedValue(request.getHeader("X-Forwarded-Port"));

        String scheme = isBlank(forwardedProto) ? request.getScheme() : forwardedProto.trim();
        String hostPort;

        if (!isBlank(forwardedHost)) {
            hostPort = forwardedHost.trim();
        } else {
            int port = resolvePort(request, forwardedPort);
            hostPort = request.getServerName();
            if (!isDefaultPort(scheme, port)) {
                hostPort = hostPort + ":" + port;
            }
        }

        if (isBlank(scheme) || isBlank(hostPort)) {
            return billingProperties.getPublicBaseUrl();
        }
        return scheme + "://" + hostPort;
    }

    private String firstForwardedValue(String value) {
        if (isBlank(value)) {
            return null;
        }
        String[] split = value.split(",");
        if (split.length == 0 || isBlank(split[0])) {
            return null;
        }
        return split[0].trim();
    }

    private int resolvePort(HttpServletRequest request, String forwardedPort) {
        if (!isBlank(forwardedPort)) {
            try {
                return Integer.parseInt(forwardedPort.trim());
            } catch (NumberFormatException ignored) {
                // Fallback to servlet request port.
            }
        }
        return request.getServerPort();
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String htmlEscape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
