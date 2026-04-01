package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrder;
import fingenie.com.fingenie.entity.SubscriptionPlan;

public interface BillingGatewayService {

    PaymentGateway supportedGateway();

    GatewayCheckoutResult createCheckout(PaymentOrder order, SubscriptionPlan plan, String clientIp, String publicBaseUrl);
}
