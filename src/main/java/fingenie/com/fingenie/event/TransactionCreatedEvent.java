package fingenie.com.fingenie.event;

import fingenie.com.fingenie.entity.Category.CategoryType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Domain event fired when a transaction is created.
 * Uses primitive/immutable data instead of entity references to ensure
 * safe async processing without Hibernate session dependencies.
 * 
 * OSIV-SAFE: This event carries all needed data as primitives,
 * allowing @Async event listeners to process without lazy loading issues.
 */
@Getter
public class TransactionCreatedEvent extends ApplicationEvent {

    private final Long transactionId;
    private final Long accountId;
    private final Long walletId;
    private final Long categoryId;
    private final String categoryName;
    private final CategoryType categoryType;
    private final BigDecimal amount;
    private final String description;
    private final Date transactionDate;

    public TransactionCreatedEvent(
            Object source,
            Long transactionId,
            Long accountId,
            Long walletId,
            Long categoryId,
            String categoryName,
            CategoryType categoryType,
            BigDecimal amount,
            String description,
            Date transactionDate
    ) {
        super(source);
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
    }

}
