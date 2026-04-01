package fingenie.com.fingenie.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for transaction events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEventPayload {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("wallet_id")
    private Long walletId;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    private BigDecimal amount;
    private String currency;
    private String description;

    @JsonProperty("transaction_date")
    private LocalDate transactionDate;

    @JsonProperty("transaction_type")
    private String transactionType; // INCOME, EXPENSE, TRANSFER

    @JsonProperty("is_recurring")
    private Boolean isRecurring;

    public static TransactionEventPayload created(
            Long userId,
            Long transactionId,
            Long accountId,
            Long walletId,
            Long categoryId,
            String categoryName,
            BigDecimal amount,
            String description,
            LocalDate transactionDate,
            String transactionType,
            String currency
    ) {
        TransactionEventPayload event = new TransactionEventPayload();
        event.setUserId(userId);
        event.setTransactionId(transactionId);
        event.setAccountId(accountId);
        event.setWalletId(walletId);
        event.setCategoryId(categoryId);
        event.setCategoryName(categoryName);
        event.setAmount(amount);
        event.setDescription(description);
        event.setTransactionDate(transactionDate);
        event.setTransactionType(transactionType);
        event.setCurrency(currency);
        event.setIsRecurring(false);
        return event;
    }

    public static TransactionEventPayload updated(
            Long userId,
            Long transactionId,
            Long accountId,
            Long walletId,
            Long categoryId,
            String categoryName,
            BigDecimal amount,
            String description,
            LocalDate transactionDate,
            String transactionType,
            String currency
    ) {
        TransactionEventPayload event = new TransactionEventPayload();
        event.setUserId(userId);
        event.setTransactionId(transactionId);
        event.setAccountId(accountId);
        event.setWalletId(walletId);
        event.setCategoryId(categoryId);
        event.setCategoryName(categoryName);
        event.setAmount(amount);
        event.setDescription(description);
        event.setTransactionDate(transactionDate);
        event.setTransactionType(transactionType);
        event.setCurrency(currency);
        event.setIsRecurring(false);
        return event;
    }

    public static TransactionEventPayload deleted(Long userId, Long transactionId) {
        TransactionEventPayload event = new TransactionEventPayload();
        event.setUserId(userId);
        event.setTransactionId(transactionId);
        return event;
    }
}
