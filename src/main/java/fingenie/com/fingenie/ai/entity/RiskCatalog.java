package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RiskCatalog - AI system risk matrix for governance and incident management.
 * 
 * Risk scoring: Impact (1-5) × Likelihood (1-5) = Risk Score (1-25)
 * 
 * Risk categories:
 * - OPERATIONAL: System availability, performance
 * - MODEL: Drift, bias, accuracy degradation
 * - DATA: Quality, privacy, integrity
 * - SECURITY: Access, breaches
 * - COMPLIANCE: Regulatory, audit
 * 
 * Priority levels based on score:
 * - CRITICAL (20-25): Immediate action required
 * - HIGH (12-19): Action within 24 hours
 * - MEDIUM (6-11): Action within 1 week
 * - LOW (1-5): Monitor and review
 */
@Entity
@Table(name = "risk_catalog", indexes = {
    @Index(name = "idx_risk_catalog_code", columnList = "risk_code"),
    @Index(name = "idx_risk_catalog_status", columnList = "status"),
    @Index(name = "idx_risk_catalog_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskCatalog extends BaseEntity {

    @Column(name = "risk_code", length = 10, unique = true, nullable = false)
    private String riskCode; // R001, R002, etc.

    @Column(name = "risk_name", length = 100, nullable = false)
    private String riskName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50, nullable = false)
    @Builder.Default
    private String category = "OPERATIONAL"; // OPERATIONAL, MODEL, DATA, SECURITY, COMPLIANCE

    @Column(name = "impact_score", nullable = false)
    private Integer impactScore; // 1-5

    @Column(name = "likelihood_score", nullable = false)
    private Integer likelihoodScore; // 1-5

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore; // Computed: impact × likelihood

    @Column(name = "priority", length = 20, nullable = false)
    @Builder.Default
    private String priority = "MEDIUM"; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "OPEN"; // OPEN, MITIGATING, ACCEPTED, CLOSED

    @Column(name = "mitigation_plan", columnDefinition = "TEXT")
    private String mitigationPlan;

    @Column(name = "mitigation_status", length = 50)
    private String mitigationStatus; // NOT_STARTED, IN_PROGRESS, COMPLETED, N/A

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "reviewer", length = 100)
    private String reviewer;

    @Column(name = "target_resolution_date")
    private LocalDateTime targetResolutionDate;

    @Column(name = "actual_resolution_date")
    private LocalDateTime actualResolutionDate;

    @Column(name = "residual_risk_score")
    private Integer residualRiskScore; // Risk score after mitigation

    @Column(name = "monitoring_frequency", length = 50)
    @Builder.Default
    private String monitoringFrequency = "WEEKLY"; // DAILY, WEEKLY, MONTHLY, QUARTERLY

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // === Helper Methods ===

    @PrePersist
    @PreUpdate
    public void calculateRiskScore() {
        if (impactScore != null && likelihoodScore != null) {
            this.riskScore = impactScore * likelihoodScore;
            this.priority = calculatePriority(this.riskScore);
        }
    }

    private String calculatePriority(int score) {
        if (score >= 20) return "CRITICAL";
        if (score >= 12) return "HIGH";
        if (score >= 6) return "MEDIUM";
        return "LOW";
    }
}
