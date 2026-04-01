package fingenie.com.fingenie.ai.event;

import fingenie.com.fingenie.entity.SpendingGuess;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when user rejects an AI spending guess.
 * Used for AI learning - negative feedback signal.
 */
@Getter
public class GuessRejectedEvent extends ApplicationEvent {

    private final Long accountId;
    private final SpendingGuess guess;
    private final String reason;

    public GuessRejectedEvent(Object source, Long accountId, SpendingGuess guess, String reason) {
        super(source);
        this.accountId = accountId;
        this.guess = guess;
        this.reason = reason;
    }
}
