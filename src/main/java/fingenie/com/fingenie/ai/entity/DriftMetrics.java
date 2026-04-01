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
 * DriftMetrics - Tracks model and data drift for AI quality monitoring.
 * 
 * Drift types monitored:
 * - Data Drift: Input feature distribution changes
 * - Concept Drift: Relationship between features and target changes
 * - Prediction Drift: Model output distribution changes
 * 
 * Metrics:
 * - KL Divergence: Measures distribution difference (threshold: 0.1)
 * - PSI (Population Stability Index): Measures population shift (threshold: 0.2)
 * - Feature-level drift scores
 * 
 * Alert triggers:
 * - KL Divergence > 0.15: Warning
 * - KL Divergence > 0.25: Critical
 * - PSI > 0.2: Requires model retraining
 */
@Entity
@Table(name = "drift_metrics", indexes = {
    @Index(name = "idx_drift_metrics_model", columnList = "model_version"),
    @Index(name = "idx_drift_metrics_date", columnList = "metric_date"),
    @Index(name = "idx_drift_metrics_alert", columnList = "alert_triggered")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriftMetrics extends BaseEntity {

    @Column(name = "model_version", length = 50, nullable = false)
    private String modelVersion;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "drift_type", length = 30, nullable = false)
    @Builder.Default
    private String driftType = "DATA"; // DATA, CONCEPT, PREDICTION

    // === Distribution Metrics ===
    
    @Column(name = "kl_divergence", precision = 10, scale = 6)
    private BigDecimal klDivergence;

    @Column(name = "psi_score", precision = 10, scale = 6)
    private BigDecimal psiScore;

    @Column(name = "js_divergence", precision = 10, scale = 6)
    private BigDecimal jsDivergence; // Jensen-Shannon divergence

    // === Feature-Level Drift ===
    
    @Column(name = "feature_drift_json", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, BigDecimal> featureDriftJson;

    @Column(name = "top_drifted_features", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> topDriftedFeatures;

    // === Prediction Drift ===
    
    @Column(name = "prediction_mean_baseline", precision = 19, scale = 4)
    private BigDecimal predictionMeanBaseline;

    @Column(name = "prediction_mean_current", precision = 19, scale = 4)
    private BigDecimal predictionMeanCurrent;

    @Column(name = "prediction_std_baseline", precision = 19, scale = 4)
    private BigDecimal predictionStdBaseline;

    @Column(name = "prediction_std_current", precision = 19, scale = 4)
    private BigDecimal predictionStdCurrent;

    // === Sample Statistics ===
    
    @Column(name = "sample_count_baseline")
    private Integer sampleCountBaseline;

    @Column(name = "sample_count_current")
    private Integer sampleCountCurrent;

    @Column(name = "baseline_start_date")
    private LocalDate baselineStartDate;

    @Column(name = "baseline_end_date")
    private LocalDate baselineEndDate;

    // === Alert Status ===
    
    @Column(name = "alert_triggered", nullable = false)
    @Builder.Default
    private Boolean alertTriggered = false;

    @Column(name = "alert_level", length = 20)
    private String alertLevel; // WARNING, CRITICAL

    @Column(name = "alert_message", length = 500)
    private String alertMessage;

    @Column(name = "requires_retraining", nullable = false)
    @Builder.Default
    private Boolean requiresRetraining = false;

    @Column(name = "retraining_scheduled_at")
    private LocalDate retrainingScheduledAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === Helper Methods ===

    @PrePersist
    @PreUpdate
    public void evaluateAlertStatus() {
        if (klDivergence != null) {
            BigDecimal criticalThreshold = new BigDecimal("0.25");
            BigDecimal warningThreshold = new BigDecimal("0.15");
            BigDecimal psiThreshold = new BigDecimal("0.20");

            if (klDivergence.compareTo(criticalThreshold) > 0) {
                this.alertTriggered = true;
                this.alertLevel = "CRITICAL";
                this.alertMessage = "KL divergence exceeds critical threshold: " + klDivergence;
                this.requiresRetraining = true;
            } else if (klDivergence.compareTo(warningThreshold) > 0) {
                this.alertTriggered = true;
                this.alertLevel = "WARNING";
                this.alertMessage = "KL divergence exceeds warning threshold: " + klDivergence;
            }

            if (psiScore != null && psiScore.compareTo(psiThreshold) > 0) {
                this.requiresRetraining = true;
                if (!Boolean.TRUE.equals(alertTriggered)) {
                    this.alertTriggered = true;
                    this.alertLevel = "WARNING";
                }
                this.alertMessage = (alertMessage != null ? alertMessage + "; " : "") + 
                    "PSI exceeds threshold: " + psiScore;
            }
        }
    }
}
