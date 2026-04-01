package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * FeatureSnapshot - Stores point-in-time feature vectors for prediction reproducibility.
 * 
 * Features captured:
 * - Rolling 7-day spend by category
 * - Category distribution percentages
 * - Volatility score
 * - Tracking frequency
 * - Emotional spending score (from surveys)
 * 
 * Use cases:
 * - Reproduce predictions exactly (debugging)
 * - Offline model training
 * - Feature drift detection
 */
@Entity
@Table(name = "feature_snapshots", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "snapshot_date"}),
    indexes = {
        @Index(name = "idx_feature_snapshot_user", columnList = "user_id"),
        @Index(name = "idx_feature_snapshot_date", columnList = "snapshot_date"),
        @Index(name = "idx_feature_snapshot_hash", columnList = "feature_hash")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureSnapshot extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "feature_hash", length = 64, nullable = false)
    private String featureHash;

    // === Online Features (Real-time computed) ===
    
    @Column(name = "rolling_7d_spend", precision = 19, scale = 2)
    private BigDecimal rolling7dSpend;

    @Column(name = "rolling_7d_spend_by_category", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, BigDecimal> rolling7dSpendByCategory;

    @Column(name = "category_distribution", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, BigDecimal> categoryDistribution;

    @Column(name = "volatility_score", precision = 5, scale = 4)
    private BigDecimal volatilityScore;

    @Column(name = "tracking_frequency", precision = 5, scale = 2)
    private BigDecimal trackingFrequency;

    @Column(name = "emotional_spending_score", precision = 5, scale = 4)
    private BigDecimal emotionalSpendingScore;

    // === Derived Features ===
    
    @Column(name = "avg_transaction_amount", precision = 19, scale = 2)
    private BigDecimal avgTransactionAmount;

    @Column(name = "transaction_count_7d")
    private Integer transactionCount7d;

    @Column(name = "weekday_vs_weekend_ratio", precision = 5, scale = 4)
    private BigDecimal weekdayVsWeekendRatio;

    @Column(name = "time_of_day_pattern", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, BigDecimal> timeOfDayPattern;

    @Column(name = "salary_cycle_phase", length = 20)
    private String salaryCyclePhase; // EARLY, MID, LATE, POST_SALARY

    // === Full Feature Vector ===
    
    @Column(name = "feature_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> featureJson;

    @Column(name = "feature_version", length = 10)
    @Builder.Default
    private String featureVersion = "v1";
}
