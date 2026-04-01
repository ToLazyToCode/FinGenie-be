package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.PaymentGateway;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingCheckoutRequest {
    @NotBlank
    private String planCode;

    private PaymentGateway gateway;
}
