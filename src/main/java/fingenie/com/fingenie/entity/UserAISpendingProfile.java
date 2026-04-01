package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_ai_spending_profile")
@Getter
@Setter
public class UserAISpendingProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "baseline_daily_spending", nullable = false, precision = 19, scale = 2, columnDefinition = "DECIMAL(19,2) DEFAULT 0.00")
    private BigDecimal baselineDailySpending = BigDecimal.ZERO;

    @Column(name = "weekday_vs_weekend_pattern", nullable = false, precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 1.00")
    private BigDecimal weekdayVsWeekendPattern = BigDecimal.ONE;

    @Column(name = "salary_cycle_pattern", length = 50)
    private String salaryCyclePattern;

    @Column(name = "category_probability_map", columnDefinition = "TEXT")
    private String categoryProbabilityMap; // JSON string

    @Column(name = "time_of_day_spending_map", columnDefinition = "TEXT")
    private String timeOfDaySpendingMap; // JSON string

    @Column(name = "volatility_score", nullable = false, precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0.50")
    private BigDecimal volatilityScore = new BigDecimal("0.50");

    @Column(name = "confidence_score", nullable = false, precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.50")
    private BigDecimal confidenceScore = new BigDecimal("0.50");

    @Column(name = "last_trained_at")
    private LocalDateTime lastTrainedAt;

    @Column(name = "prediction_accuracy_rate", nullable = false, precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.00")
    private BigDecimal predictionAccuracyRate = BigDecimal.ZERO;

    @Column(name = "total_predictions", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalPredictions = 0;

    @Column(name = "accepted_predictions", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer acceptedPredictions = 0;

    @Column(name = "rejected_predictions", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer rejectedPredictions = 0;

    @Column(name = "edited_predictions", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer editedPredictions = 0;
}
