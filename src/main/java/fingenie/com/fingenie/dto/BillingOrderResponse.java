package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingOrderResponse {
    private String orderCode;
    private String planCode;
    private String planTitle;
    private PaymentGateway gateway;
    private Long amount;
    private PaymentOrderStatus status;
    private String checkoutUrl;
    private String gatewayOrderRef;
    private String gatewayTransactionRef;
    private Timestamp paidAt;
    private Timestamp expiresAt;
}
