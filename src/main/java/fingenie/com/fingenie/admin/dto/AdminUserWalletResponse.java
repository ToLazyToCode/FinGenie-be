package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Summary of one wallet belonging to a user (admin view).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserWalletResponse {

    private Long walletId;
    private String walletName;
    private BigDecimal balance;

    /** Always "VND" for now (currency not stored on Wallet entity). */
    private String currency;

    private boolean isDefault;
    private Timestamp createdAt;
}
