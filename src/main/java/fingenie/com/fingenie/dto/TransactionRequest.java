package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class TransactionRequest {

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    @NotNull(message = "Transaction date is required")
    private Date transactionDate;
}
