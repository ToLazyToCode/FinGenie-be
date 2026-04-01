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
public class TransactionResponse {
    private Long transactionId;
    private Long accountId;
    private Long walletId;
    private String walletName;
    private Long categoryId;
    private String categoryName;
    private String categoryType;
    private BigDecimal amount;
    private String transactionType;
    private String description;
    private Date transactionDate;
    private Timestamp createdAt;
}
