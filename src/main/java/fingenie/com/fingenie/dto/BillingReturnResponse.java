package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingReturnResponse {
    private String orderCode;
    private PaymentGateway gateway;
    private PaymentOrderStatus status;
    private String message;
}
