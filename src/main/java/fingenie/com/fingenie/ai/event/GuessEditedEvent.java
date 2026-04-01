package fingenie.com.fingenie.ai.event;

import fingenie.com.fingenie.entity.SpendingGuess;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * Event published when user edits an AI spending guess before accepting.
 * Contains delta information for AI learning.
 */
@Getter
public class GuessEditedEvent extends ApplicationEvent {

    private final Long accountId;
    private final SpendingGuess guess;
    private final BigDecimal finalAmount;
    private final Long finalCategoryId;

    public GuessEditedEvent(Object source, Long accountId, SpendingGuess guess, 
                           BigDecimal finalAmount, Long finalCategoryId) {
        super(source);
        this.accountId = accountId;
        this.guess = guess;
        this.finalAmount = finalAmount;
        this.finalCategoryId = finalCategoryId;
    }

    /**
     * Calculate amount difference (positive = user increased, negative = user decreased)
     */
    public BigDecimal getAmountDelta() {
        if (finalAmount == null || guess.getGuessedAmount() == null) {
            return BigDecimal.ZERO;
        }
        return finalAmount.subtract(guess.getGuessedAmount());
    }

    /**
     * Check if category was changed
     */
    public boolean isCategoryChanged() {
        Long originalCategoryId = guess.getCategory() != null ? guess.getCategory().getId() : null;
        if (originalCategoryId == null && finalCategoryId == null) return false;
        if (originalCategoryId == null || finalCategoryId == null) return true;
        return !originalCategoryId.equals(finalCategoryId);
    }
}
