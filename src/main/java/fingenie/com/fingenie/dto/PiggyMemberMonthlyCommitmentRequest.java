package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PiggyMemberMonthlyCommitmentRequest {

    @NotNull(message = "Monthly commitment is required")
    @DecimalMin(value = "0.00", message = "Monthly commitment must be >= 0")
    private BigDecimal monthlyCommitment;
}
