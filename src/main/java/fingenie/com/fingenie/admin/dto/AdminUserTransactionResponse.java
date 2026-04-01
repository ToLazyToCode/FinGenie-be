package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Single transaction entry in a user's transaction history (admin view).
 * Mirrors AdminRecentTransactionResponse but is scoped to one user
 * and adds wallet information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserTransactionResponse {

    private Long transactionId;

    /** Signed amount: positive = income, negative = expense. */
    private BigDecimal amount;

    private String description;
    private String categoryName;
    private String categoryType;   // INCOME | EXPENSE | SAVING
    private String walletName;
    private Date transactionDate;
}
