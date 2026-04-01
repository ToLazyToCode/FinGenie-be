package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.PiggyBankMember;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PiggyBankMemberRequest {
    
    @NotNull(message = "Piggy bank ID is required")
    private Long piggyId;
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    private PiggyBankMember.MemberRole role;

    @Min(value = 1, message = "Share weight must be at least 1")
    private Integer shareWeight;

    @DecimalMin(value = "0.00", message = "Monthly commitment must be >= 0")
    private BigDecimal monthlyCommitment;
}
