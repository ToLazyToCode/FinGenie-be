package fingenie.com.fingenie.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Survey Event - Financial surveys and feedback for AI learning.
 * 
 * Event types:
 * - SURVEY_COMPLETED: User completed a financial survey
 * - FEEDBACK_SUBMITTED: User provided prediction feedback
 * - EMOTIONAL_CHECK_IN: Emotional spending mood check
 * - GOAL_FEEDBACK: Savings goal progress feedback
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyEvent {

    @JsonProperty("schema_version")
    @Builder.Default
    private String schemaVersion = "v1";

    @JsonProperty("event_id")
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("event_type")
    private String eventType;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("idempotency_key")
    @Builder.Default
    private String idempotencyKey = UUID.randomUUID().toString();

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("source_service")
    @Builder.Default
    private String sourceService = "fingenie-backend";

    // === User Context ===
    
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("survey_id")
    private String surveyId;

    @JsonProperty("survey_type")
    private String surveyType;

    // === Survey Response ===
    
    @JsonProperty("responses")
    private Map<String, Object> responses;

    @JsonProperty("completion_time_seconds")
    private Integer completionTimeSeconds;

    // === Emotional Spending Specific ===
    
    @JsonProperty("emotional_score")
    private BigDecimal emotionalScore; // 0-1 scale

    @JsonProperty("mood")
    private String mood; // HAPPY, STRESSED, ANXIOUS, NEUTRAL, etc.

    @JsonProperty("spending_trigger")
    private String spendingTrigger; // What triggered spending

    // === Prediction Feedback Specific ===
    
    @JsonProperty("prediction_id")
    private Long predictionId;

    @JsonProperty("feedback_type")
    private String feedbackType; // ACCEPT, REJECT, EDIT

    @JsonProperty("original_amount")
    private BigDecimal originalAmount;

    @JsonProperty("corrected_amount")
    private BigDecimal correctedAmount;

    @JsonProperty("feedback_reason")
    private String feedbackReason;

    // === Factory Methods ===

    public static SurveyEvent emotionalCheckIn(Long userId, BigDecimal emotionalScore, 
            String mood, String trigger) {
        return SurveyEvent.builder()
                .eventType("EMOTIONAL_CHECK_IN")
                .userId(userId)
                .surveyType("EMOTIONAL_SPENDING")
                .emotionalScore(emotionalScore)
                .mood(mood)
                .spendingTrigger(trigger)
                .build();
    }

    public static SurveyEvent predictionFeedback(Long userId, Long predictionId, 
            String feedbackType, BigDecimal originalAmount, BigDecimal correctedAmount, 
            String reason) {
        return SurveyEvent.builder()
                .eventType("FEEDBACK_SUBMITTED")
                .userId(userId)
                .surveyType("PREDICTION_FEEDBACK")
                .predictionId(predictionId)
                .feedbackType(feedbackType)
                .originalAmount(originalAmount)
                .correctedAmount(correctedAmount)
                .feedbackReason(reason)
                .build();
    }

    public static SurveyEvent surveyCompleted(Long userId, String surveyId, 
            String surveyType, Map<String, Object> responses) {
        return SurveyEvent.builder()
                .eventType("SURVEY_COMPLETED")
                .userId(userId)
                .surveyId(surveyId)
                .surveyType(surveyType)
                .responses(responses)
                .build();
    }
}
