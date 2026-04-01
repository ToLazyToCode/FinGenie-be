package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PiggyBankResponse {
    private Long piggyId;
    private Long walletId;
    private BigDecimal goalAmount;
    private Date lockUntil;
    private BigDecimal interestRate;
    private BigDecimal withdrawalPenaltyRate;
    private boolean isShared;
    private Timestamp createdAt;
}
