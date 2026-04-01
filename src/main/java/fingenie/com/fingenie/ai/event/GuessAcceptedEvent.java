package fingenie.com.fingenie.ai.event;

import fingenie.com.fingenie.entity.SpendingGuess;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when user accepts an AI spending guess.
 * Used for AI learning and gamification triggers.
 */
@Getter
public class GuessAcceptedEvent extends ApplicationEvent {

    private final Long accountId;
    private final SpendingGuess guess;

    public GuessAcceptedEvent(Object source, Long accountId, SpendingGuess guess) {
        super(source);
        this.accountId = accountId;
        this.guess = guess;
    }
}
