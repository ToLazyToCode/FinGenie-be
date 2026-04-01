package fingenie.com.fingenie.survey.entity;

import fingenie.com.fingenie.base.BaseEntity;
import fingenie.com.fingenie.survey.enums.BehavioralSegment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * User Behavior Profile - Computed behavioral profile from survey responses.
 * 
 * Stores:
 * - Deterministic scores (computed in Spring Boot)
 * - Behavioral segment classification
 * - Explanation factors for transparency
 * - Action plan for user guidance
 */
@Entity
@Table(name = "user_behavior_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehaviorProfile extends BaseEntity {
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_response_id")
    private UserSurveyResponse surveyResponse;
    
    @Column(name = "profile_version", nullable = false)
    @Builder.Default
    private Integer profileVersion = 1;
    
    // ====== Computed Scores (0-100 scale) ======
    
    @Column(name = "overspending_score", precision = 5, scale = 2)
    private BigDecimal overspendingScore;
    
    @Column(name = "debt_risk_score", precision = 5, scale = 2)
    private BigDecimal debtRiskScore;
    
    @Column(name = "savings_capacity_score", precision = 5, scale = 2)
    private BigDecimal savingsCapacityScore;
    
    @Column(name = "financial_anxiety_index", precision = 5, scale = 2)
    private BigDecimal financialAnxietyIndex;
    
    // ====== Segment Classification ======
    
    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false, length = 50)
    private BehavioralSegment segment;
    
    @Column(name = "segment_confidence", precision = 3, scale = 2)
    private BigDecimal segmentConfidence;
    
    // ====== Explainability ======
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "explanation_factors", columnDefinition = "TEXT")
    private Map<String, Object> explanationFactors;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "first_action_plan", columnDefinition = "TEXT")
    private Map<String, Object> firstActionPlan;
    
    // ====== Feature Vector for AI ======
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_vector", columnDefinition = "TEXT")
    private Map<String, Double> featureVector;
    
    // ====== Timestamps ======
    
    @Column(name = "survey_completed_at")
    private LocalDateTime surveyCompletedAt;
    
    @Column(name = "synced_to_ai_at")
    private LocalDateTime syncedToAiAt;
    
    /**
     * Update profile with new scores.
     */
    public void updateScores(BigDecimal overspending, BigDecimal debtRisk, 
                            BigDecimal savingsCapacity, BigDecimal anxietyIndex) {
        this.overspendingScore = overspending;
        this.debtRiskScore = debtRisk;
        this.savingsCapacityScore = savingsCapacity;
        this.financialAnxietyIndex = anxietyIndex;
    }
    
    /**
     * Calculate overall risk level based on scores.
     */
    public String getRiskLevel() {
        double avgRisk = (overspendingScore.doubleValue() + debtRiskScore.doubleValue()) / 2;
        if (avgRisk >= 70) return "HIGH";
        if (avgRisk >= 40) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Check if profile needs refresh (older than 90 days).
     */
    public boolean needsRefresh() {
        if (surveyCompletedAt == null) return true;
        return surveyCompletedAt.isBefore(LocalDateTime.now().minusDays(90));
    }
}

