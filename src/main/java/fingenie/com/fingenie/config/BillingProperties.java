package fingenie.com.fingenie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "billing")
@Getter
@Setter
public class BillingProperties {

    private String publicBaseUrl = "http://localhost:8080";
    private String mobileReturnUrl = "fingenie://redirect/payment";
    private boolean returnPageAutoRedirectEnabled = true;
    private int returnPageAutoRedirectDelayMs = 1200;
    private int checkoutExpireMinutes = 15;
    private int gatewayTimeoutMs = 5000;

    private final PayOS payos = new PayOS();
    private final VNPay vnpay = new VNPay();

    @Getter
    @Setter
    public static class PayOS {
        private boolean enabled = false;
        private String baseUrl = "https://api-merchant.payos.vn";
        private String clientId = "";
        private String apiKey = "";
        private String checksumKey = "";
        private String createPath = "/v2/payment-requests";
        private String returnPath = "/api/v1/billing/return/payos";
        private String cancelPath = "/api/v1/billing/return/payos";
        private String webhookPath = "/api/v1/billing/webhooks/payos";
    }

    @Getter
    @Setter
    public static class VNPay {
        private boolean enabled = false;
        private String tmnCode = "";
        private String hashSecret = "";
        private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        private String version = "2.1.0";
        private String command = "pay";
        private String orderType = "other";
        private String locale = "vn";
        private String currCode = "VND";
        private String returnPath = "/api/v1/billing/return/vnpay";
        private String ipnPath = "/api/v1/billing/ipn/vnpay";
    }
}
