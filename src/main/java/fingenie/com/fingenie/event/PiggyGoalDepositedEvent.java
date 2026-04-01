package fingenie.com.fingenie.event;

import java.math.BigDecimal;
import org.springframework.context.ApplicationEvent;

public class PiggyGoalDepositedEvent extends ApplicationEvent {

    private final Long accountId;
    private final Long goalId;
    private final BigDecimal amount;

    public PiggyGoalDepositedEvent(Object source, Long accountId, Long goalId, BigDecimal amount) {
        super(source);
        this.accountId = accountId;
        this.goalId = goalId;
        this.amount = amount;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
