package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long walletId;
    private Long accountId;
    private String walletName;
    private BigDecimal balance;
    private boolean isDefault;
    private Timestamp createdAt;
}
