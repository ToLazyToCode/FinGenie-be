package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecentTransactionResponse {

    private Long transactionId;
    private Long accountId;
    private String accountEmail;
    private String accountName;

    /** Signed amount: positive = income, negative = expense */
    private BigDecimal amount;

    private String description;
    private String categoryName;
    private LocalDate transactionDate;
}
