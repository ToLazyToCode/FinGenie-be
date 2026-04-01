package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AlertLog - Tracks AI system alerts and incidents for operational monitoring.
 * 
 * Severity levels (incident protocol):
 * - Level 1 (MINOR): Single timeout, transient errors
 * - Level 2 (MAJOR): Multiple failures, degraded performance
 * - Level 3 (CRITICAL): System down, data integrity issues
 * 
 * Alert types:
 * - AI_SERVICE_DOWN: AI inference service unavailable
 * - PREDICTION_SPIKE: Unusual prediction rate
 * - DRIFT_DETECTED: Model drift threshold exceeded
 * - HIGH_RISK_SURGE: Increase in high-risk predictions
 * - LATENCY_HIGH: Inference latency exceeds threshold
 * - ERROR_RATE_HIGH: Error rate exceeds threshold
 * - KAFKA_LAG: Consumer lag exceeds threshold
 */
@Entity
@Table(name = "alert_log", indexes = {
    @Index(name = "idx_alert_log_type", columnList = "alert_type"),
    @Index(name = "idx_alert_log_severity", columnList = "severity_level"),
    @Index(name = "idx_alert_log_triggered", columnList = "triggered_at"),
    @Index(name = "idx_alert_log_resolved", columnList = "resolved_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLog extends BaseEntity {

    @Column(name = "alert_type", length = 50, nullable = false)
    private String alertType;

    @Column(name = "severity_level", nullable = false)
    private Integer severityLevel; // 1 = Minor, 2 = Major, 3 = Critical

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "source", length = 100)
    private String source; // Service/component that triggered alert

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "OPEN"; // OPEN, ACKNOWLEDGED, INVESTIGATING, RESOLVED, FALSE_POSITIVE

    @Column(name = "escalated", nullable = false)
    @Builder.Default
    private Boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalated_to", length = 100)
    private String escalatedTo;

    @Column(name = "auto_remediation_applied", nullable = false)
    @Builder.Default
    private Boolean autoRemediationApplied = false;

    @Column(name = "remediation_action", length = 200)
    private String remediationAction;

    @Column(name = "related_risk_code", length = 10)
    private String relatedRiskCode; // FK to RiskCatalog

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    // === Helper Methods ===

    public void acknowledge(String by) {
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = by;
        this.status = "ACKNOWLEDGED";
    }

    public void resolve(String by, String notes) {
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = by;
        this.resolutionNotes = notes;
        this.status = "RESOLVED";
    }

    public void escalate(String to) {
        this.escalated = true;
        this.escalatedAt = LocalDateTime.now();
        this.escalatedTo = to;
    }

    public void markFalsePositive(String by, String notes) {
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = by;
        this.resolutionNotes = notes;
        this.status = "FALSE_POSITIVE";
    }

    public long getDurationMinutes() {
        LocalDateTime endTime = resolvedAt != null ? resolvedAt : LocalDateTime.now();
        return java.time.Duration.between(triggeredAt, endTime).toMinutes();
    }
}
