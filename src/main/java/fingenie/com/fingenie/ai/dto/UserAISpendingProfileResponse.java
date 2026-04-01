package fingenie.com.fingenie.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for UserAISpendingProfile entity responses.
 * OSIV-SAFE: Fully serializable without Hibernate session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAISpendingProfileResponse {

    private Long id;
    private Long userId;
    private BigDecimal baselineDailySpending;
    private BigDecimal weekdayVsWeekendPattern;
    private String salaryCyclePattern;
    private String categoryProbabilityMap;
    private String timeOfDaySpendingMap;
    private BigDecimal volatilityScore;
    private BigDecimal confidenceScore;
    private LocalDateTime lastTrainedAt;
    private BigDecimal predictionAccuracyRate;
    private Integer totalPredictions;
    private Integer acceptedPredictions;

    /**
     * Factory method to convert entity to DTO within transactional boundary.
     */
    public static UserAISpendingProfileResponse fromEntity(
            fingenie.com.fingenie.entity.UserAISpendingProfile entity) {
        if (entity == null) {
            return null;
        }
        return UserAISpendingProfileResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .baselineDailySpending(entity.getBaselineDailySpending())
                .weekdayVsWeekendPattern(entity.getWeekdayVsWeekendPattern())
                .salaryCyclePattern(entity.getSalaryCyclePattern())
                .categoryProbabilityMap(entity.getCategoryProbabilityMap())
                .timeOfDaySpendingMap(entity.getTimeOfDaySpendingMap())
                .volatilityScore(entity.getVolatilityScore())
                .confidenceScore(entity.getConfidenceScore())
                .lastTrainedAt(entity.getLastTrainedAt())
                .predictionAccuracyRate(entity.getPredictionAccuracyRate())
                .totalPredictions(entity.getTotalPredictions())
                .acceptedPredictions(entity.getAcceptedPredictions())
                .build();
    }
}
