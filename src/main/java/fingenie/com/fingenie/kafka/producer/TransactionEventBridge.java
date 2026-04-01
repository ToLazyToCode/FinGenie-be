package fingenie.com.fingenie.kafka.producer;

import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.event.TransactionCreatedEvent;
import fingenie.com.fingenie.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventBridge {

    private static final String DEFAULT_CURRENCY = "VND";
    private final EventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        TransactionEvent kafkaEvent = TransactionEvent.created(
                event.getAccountId(),
                event.getTransactionId(),
                event.getAccountId(),
                event.getWalletId(),
                event.getCategoryId(),
                event.getCategoryName(),
                event.getAmount(),
                event.getDescription(),
                event.getTransactionDate().toLocalDate(),
                mapTransactionType(event.getCategoryType()),
                DEFAULT_CURRENCY
        );

        eventPublisher.publishTransactionCreated(kafkaEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Failed to publish TRANSACTION_CREATED event for account {} tx {}",
                                event.getAccountId(),
                                event.getTransactionId(),
                                ex
                        );
                    } else {
                        log.debug(
                                "Published TRANSACTION_CREATED event for account {} tx {} partition={} offset={}",
                                event.getAccountId(),
                                event.getTransactionId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }

    private String mapTransactionType(Category.CategoryType categoryType) {
        if (categoryType == Category.CategoryType.INCOME) {
            return "INCOME";
        }
        return "EXPENSE";
    }
}
