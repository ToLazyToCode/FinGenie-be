package fingenie.com.fingenie.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaction Event - Published to Kafka when transactions are created/updated/deleted.
 *
 * Used for:
 * - Feature store updates (recalculate user spending patterns)
 * - AI prediction triggers
 * - Audit logging
 */
@Data
@SuperBuilder(builderMethodName = "builder")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransactionEvent extends BaseEvent {

    /**
     * Transaction event payload data.
     */
    private TransactionEventPayload payload;

    /**
     * Creates a TRANSACTION_CREATED event.
     */
    public static TransactionEvent created(
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
        return TransactionEvent.builder()
                .eventType("TRANSACTION_CREATED")
                .sourceService("fingenie-backend")
                .payload(TransactionEventPayload.created(
                        userId,
                        transactionId,
                        accountId,
                        walletId,
                        categoryId,
                        categoryName,
                        amount,
                        description,
                        transactionDate,
                        transactionType,
                        currency
                ))
                .timestamp(java.time.Instant.now())
                .build();
    }

    /**
     * Creates a TRANSACTION_UPDATED event.
     */
    public static TransactionEvent updated(
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
        return TransactionEvent.builder()
                .eventType("TRANSACTION_UPDATED")
                .sourceService("fingenie-backend")
                .payload(TransactionEventPayload.updated(
                        userId,
                        transactionId,
                        accountId,
                        walletId,
                        categoryId,
                        categoryName,
                        amount,
                        description,
                        transactionDate,
                        transactionType,
                        currency
                ))
                .timestamp(java.time.Instant.now())
                .build();
    }

    /**
     * Creates a TRANSACTION_DELETED event.
     */
    public static TransactionEvent deleted(Long userId, Long transactionId) {
        return TransactionEvent.builder()
                .eventType("TRANSACTION_DELETED")
                .sourceService("fingenie-backend")
                .payload(TransactionEventPayload.deleted(userId, transactionId))
                .timestamp(java.time.Instant.now())
                .build();
    }
}
