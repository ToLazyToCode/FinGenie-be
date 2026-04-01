package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PredictionAuditLog - Stores audit trail for all AI predictions.
 * 
 * Captures:
 * - Model version used for the prediction
 * - Feature snapshot hash for reproducibility
 * - Explanation vector (top 3 feature impacts)
 * - Performance metrics (latency, fallback usage)
 * 
 * This enables:
 * - Full prediction reproducibility
 * - Model performance tracking
 * - Regulatory compliance auditing
 */
@Entity
@Table(name = "prediction_audit_log", indexes = {
    @Index(name = "idx_prediction_audit_user", columnList = "user_id"),
    @Index(name = "idx_prediction_audit_model", columnList = "model_version"),
    @Index(name = "idx_prediction_audit_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionAuditLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "prediction_id")
    private Long predictionId;

    @Column(name = "prediction_type", length = 50, nullable = false)
    @Builder.Default
    private String predictionType = "SPENDING_GUESS"; // SPENDING_GUESS, CATEGORY_PREDICTION, RISK_SCORE

    @Column(name = "model_version", length = 50, nullable = false)
    private String modelVersion;

    @Column(name = "feature_snapshot_hash", length = 64, nullable = false)
    private String featureSnapshotHash;

    @Column(name = "explanation_vector", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> explanationVector;

    @Column(name = "raw_score", precision = 10, scale = 6)
    private BigDecimal rawScore;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "inference_latency_ms")
    private Integer inferenceLatencyMs;

    @Column(name = "fallback_used", nullable = false)
    @Builder.Default
    private Boolean fallbackUsed = false;

    @Column(name = "fallback_reason", length = 200)
    private String fallbackReason;

    @Column(name = "request_context", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> requestContext;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> responsePayload;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;
}
