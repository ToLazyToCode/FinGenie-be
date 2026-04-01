package fingenie.com.fingenie.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Model Prediction Event - Published by AI Service, consumed by Spring Boot.
 * 
 * Contains:
 * - Prediction results with confidence scores
 * - Model version and feature hash for reproducibility
 * - Explanation vector (top factors)
 * - Performance metrics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelPredictionEvent {

    @JsonProperty("schema_version")
    @Builder.Default
    private String schemaVersion = "v1";

    @JsonProperty("event_id")
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "MODEL_PREDICTION";

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("source_service")
    @Builder.Default
    private String sourceService = "fingenie-ai-service";

    // === Prediction Context ===
    
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("prediction_id")
    private Long predictionId;

    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("prediction_type")
    private String predictionType; // SPENDING_GUESS, CATEGORY_PREDICTION, RISK_SCORE

    @JsonProperty("request_timestamp")
    private Instant requestTimestamp;

    // === Model Information ===
    
    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("feature_hash")
    private String featureHash;

    @JsonProperty("feature_version")
    private String featureVersion;

    // === Prediction Results ===
    
    @JsonProperty("predicted_amount")
    private BigDecimal predictedAmount;

    @JsonProperty("predicted_category")
    private String predictedCategory;

    @JsonProperty("predicted_category_id")
    private Long predictedCategoryId;

    @JsonProperty("confidence")
    private BigDecimal confidence;

    @JsonProperty("raw_score")
    private BigDecimal rawScore;

    // === Explanation ===
    
    @JsonProperty("risk_score")
    private BigDecimal riskScore;

    @JsonProperty("explanation")
    private ExplanationVector explanation;

    // === Performance Metrics ===
    
    @JsonProperty("inference_latency_ms")
    private Integer inferenceLatencyMs;

    @JsonProperty("feature_fetch_latency_ms")
    private Integer featureFetchLatencyMs;

    @JsonProperty("total_latency_ms")
    private Integer totalLatencyMs;

    // === Status ===
    
    @JsonProperty("fallback_used")
    @Builder.Default
    private Boolean fallbackUsed = false;

    @JsonProperty("fallback_reason")
    private String fallbackReason;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * Explanation vector containing top contributing factors.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExplanationVector {
        
        @JsonProperty("risk_score")
        private BigDecimal riskScore;

        private List<FeatureImpact> factors;

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("improvement_suggestions")
        private List<String> improvementSuggestions;
    }

    /**
     * Individual feature impact on prediction.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeatureImpact {
        
        private String feature;
        private BigDecimal impact;
        private String direction; // POSITIVE, NEGATIVE
        private BigDecimal featureValue;

        @JsonProperty("contribution_percent")
        private BigDecimal contributionPercent;
    }
}
