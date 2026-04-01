package fingenie.com.fingenie.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event fired when a transaction is deleted.
 * Carries only primitive identifiers for async-safe processing.
 */
@Getter
public class TransactionDeletedEvent extends ApplicationEvent {

    private final Long transactionId;
    private final Long accountId;

    public TransactionDeletedEvent(Object source, Long transactionId, Long accountId) {
        super(source);
        this.transactionId = transactionId;
        this.accountId = accountId;
    }
}

