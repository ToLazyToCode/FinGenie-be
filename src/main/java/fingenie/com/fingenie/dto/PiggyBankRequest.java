package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class PiggyBankRequest {
    
    @NotNull(message = "Wallet ID is required")
    private Long walletId;
    
    @NotNull(message = "Goal amount is required")
    private BigDecimal goalAmount;
    
    private Date lockUntil;
    
    private BigDecimal interestRate;
    
    private BigDecimal withdrawalPenaltyRate;
    
    private boolean isShared;
}
