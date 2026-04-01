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
public class BillingCheckoutResponse {
    private String orderCode;
    private String planCode;
    private PaymentGateway gateway;
    private Long amount;
    private PaymentOrderStatus status;
    private String checkoutUrl;
    private Timestamp expiresAt;
}
