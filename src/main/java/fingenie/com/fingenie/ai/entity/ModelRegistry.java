package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * ModelRegistry - Tracks deployed AI models and their lifecycle.
 * 
 * Lifecycle stages:
 * - TRAINING: Model is being trained
 * - STAGED: Ready for shadow testing
 * - SHADOW: Running in shadow mode (predictions logged but not served)
 * - PRODUCTION: Active in production
 * - ARCHIVED: Retired but preserved for reproducibility
 * - ROLLBACK: Marked as rollback target
 * 
 * Integrates with MLflow for:
 * - Experiment tracking
 * - Model versioning
 * - Artifact storage
 */
@Entity
@Table(name = "model_registry", indexes = {
    @Index(name = "idx_model_registry_name", columnList = "model_name"),
    @Index(name = "idx_model_registry_status", columnList = "status"),
    @Index(name = "idx_model_registry_version", columnList = "model_version")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelRegistry extends BaseEntity {

    @Column(name = "model_name", length = 100, nullable = false)
    private String modelName;

    @Column(name = "model_version", length = 50, nullable = false)
    private String modelVersion;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "STAGED"; // TRAINING, STAGED, SHADOW, PRODUCTION, ARCHIVED, ROLLBACK

    @Column(name = "model_type", length = 50)
    @Builder.Default
    private String modelType = "SPENDING_PREDICTOR"; // SPENDING_PREDICTOR, CATEGORY_CLASSIFIER, RISK_SCORER

    @Column(name = "mlflow_run_id", length = 100)
    private String mlflowRunId;

    @Column(name = "mlflow_experiment_id", length = 100)
    private String mlflowExperimentId;

    @Column(name = "artifact_uri", length = 500)
    private String artifactUri;

    // === Training Metadata ===
    
    @Column(name = "training_dataset_version", length = 50)
    private String trainingDatasetVersion;

    @Column(name = "feature_version", length = 50)
    private String featureVersion;

    @Column(name = "hyperparameters", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> hyperparameters;

    @Column(name = "training_started_at")
    private LocalDateTime trainingStartedAt;

    @Column(name = "training_completed_at")
    private LocalDateTime trainingCompletedAt;

    // === Validation Metrics ===
    
    @Column(name = "metrics_json", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metricsJson;

    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "mae", precision = 19, scale = 4)
    private BigDecimal mae; // Mean Absolute Error

    @Column(name = "rmse", precision = 19, scale = 4)
    private BigDecimal rmse; // Root Mean Square Error

    @Column(name = "f1_score", precision = 5, scale = 4)
    private BigDecimal f1Score;

    // === Deployment Tracking ===
    
    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "deployed_by", length = 100)
    private String deployedBy;

    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;

    @Column(name = "rolled_back_by", length = 100)
    private String rolledBackBy;

    @Column(name = "rollback_reason", length = 500)
    private String rollbackReason;

    @Column(name = "previous_version", length = 50)
    private String previousVersion;

    // === Shadow Testing ===
    
    @Column(name = "shadow_started_at")
    private LocalDateTime shadowStartedAt;

    @Column(name = "shadow_ended_at")
    private LocalDateTime shadowEndedAt;

    @Column(name = "shadow_metrics", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> shadowMetrics;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
