package fingenie.com.fingenie.survey.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * User behavioral segments derived from survey responses.
 * 
 * Classification priority (highest first):
 * 1. FINANCIALLY_AT_RISK
 * 2. IMPULSE_SPENDER
 * 3. HIGH_EARNER_LOW_CONTROL
 * 4. FINANCIALLY_ANXIOUS_BEGINNER
 * 5. STABLE_BUILDER
 * 6. MODERATE_MANAGER (default)
 */
@Getter
@RequiredArgsConstructor
public enum BehavioralSegment {
    
    FINANCIALLY_AT_RISK(
        "Financially At Risk",
        "High debt burden with limited savings capacity",
        1
    ),
    
    IMPULSE_SPENDER(
        "Impulse Spender",
        "Frequent unplanned purchases affecting financial stability",
        2
    ),
    
    HIGH_EARNER_LOW_CONTROL(
        "High Earner, Low Control",
        "Good income but spending exceeds reasonable bounds",
        3
    ),
    
    FINANCIALLY_ANXIOUS_BEGINNER(
        "Financially Anxious Beginner",
        "Limited financial knowledge causing money-related stress",
        4
    ),
    
    STABLE_BUILDER(
        "Stable Builder",
        "Good savings habits and controlled spending patterns",
        5
    ),
    
    MODERATE_MANAGER(
        "Moderate Manager",
        "Balanced financial behavior with room for optimization",
        6
    );
    
    private final String displayName;
    private final String description;
    private final int priority;
}
