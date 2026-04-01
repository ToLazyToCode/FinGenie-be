package fingenie.com.fingenie.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Feature Update Event - Notifies when user features are updated.
 * 
 * Published when:
 * - Transaction triggers feature recalculation
 * - Daily batch feature snapshot is created
 * - Manual feature refresh is triggered
 * 
 * Consumed by:
 * - AI Service for real-time feature fetching
 * - Feature store for caching
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureUpdateEvent {

    @JsonProperty("schema_version")
    @Builder.Default
    private String schemaVersion = "v1";

    @JsonProperty("event_id")
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "FEATURE_UPDATE";

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("idempotency_key")
    @Builder.Default
    private String idempotencyKey = UUID.randomUUID().toString();

    @JsonProperty("source_service")
    @Builder.Default
    private String sourceService = "fingenie-backend";

    // === User Context ===
    
    @JsonProperty("user_id")
    private Long userId;

    // === Feature Metadata ===
    
    @JsonProperty("feature_hash")
    private String featureHash;

    @JsonProperty("feature_version")
    @Builder.Default
    private String featureVersion = "v1";

    @JsonProperty("update_type")
    private String updateType; // INCREMENTAL, FULL_REFRESH, BATCH

    @JsonProperty("trigger_event")
    private String triggerEvent; // TRANSACTION_CREATED, DAILY_BATCH, MANUAL

    @JsonProperty("trigger_event_id")
    private String triggerEventId;

    // === Feature Summary ===
    
    @JsonProperty("features_updated")
    private Map<String, Boolean> featuresUpdated;

    @JsonProperty("rolling_7d_spend")
    private BigDecimal rolling7dSpend;

    @JsonProperty("volatility_score")
    private BigDecimal volatilityScore;

    @JsonProperty("tracking_frequency")
    private BigDecimal trackingFrequency;

    @JsonProperty("emotional_spending_score")
    private BigDecimal emotionalSpendingScore;

    // === Cache Control ===
    
    @JsonProperty("cache_ttl_seconds")
    @Builder.Default
    private Integer cacheTtlSeconds = 86400; // 24 hours

    @JsonProperty("invalidate_previous")
    @Builder.Default
    private Boolean invalidatePrevious = true;

    // === Factory Methods ===

    public static FeatureUpdateEvent fromTransaction(Long userId, String featureHash, 
            String transactionEventId) {
        return FeatureUpdateEvent.builder()
                .userId(userId)
                .featureHash(featureHash)
                .updateType("INCREMENTAL")
                .triggerEvent("TRANSACTION_CREATED")
                .triggerEventId(transactionEventId)
                .build();
    }

    public static FeatureUpdateEvent dailyBatch(Long userId, String featureHash, 
            Map<String, Boolean> featuresUpdated) {
        return FeatureUpdateEvent.builder()
                .userId(userId)
                .featureHash(featureHash)
                .updateType("FULL_REFRESH")
                .triggerEvent("DAILY_BATCH")
                .featuresUpdated(featuresUpdated)
                .build();
    }
}
