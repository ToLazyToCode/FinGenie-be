package fingenie.com.fingenie.survey.service;

import fingenie.com.fingenie.survey.entity.UserBehaviorProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka event publisher for survey events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyEventPublisher {
    
    private static final String SURVEY_EVENTS_TOPIC = "survey-events";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Publish SURVEY_COMPLETED event to Kafka.
     * This triggers AI service to sync baseline features.
     */
    public void publishSurveyCompleted(Long userId, Long responseId) {
        publishSurveyCompleted(userId, responseId, null);
    }

    /**
     * Publish SURVEY_COMPLETED event with optional computed profile snapshot.
     */
    public void publishSurveyCompleted(Long userId, Long responseId, UserBehaviorProfile profile) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SURVEY_COMPLETED");
        event.put("userId", userId);
        event.put("surveyResponseId", responseId);
        event.put("timestamp", Instant.now().toString());
        event.put("version", "1.0");

        if (profile != null) {
            event.put("overspendingScore", profile.getOverspendingScore());
            event.put("debtRiskScore", profile.getDebtRiskScore());
            event.put("savingsCapacityScore", profile.getSavingsCapacityScore());
            event.put("financialAnxietyIndex", profile.getFinancialAnxietyIndex());
            event.put("segment", profile.getSegment() != null ? profile.getSegment().name() : null);
            event.put("segmentConfidence", profile.getSegmentConfidence());
            event.put("riskLevel", profile.getRiskLevel());
            event.put("surveyCompletedAt",
                    profile.getSurveyCompletedAt() != null ? profile.getSurveyCompletedAt().toString() : null);
        }
        
        try {
            kafkaTemplate.send(SURVEY_EVENTS_TOPIC, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SURVEY_COMPLETED event for user {}", userId, ex);
                    } else {
                        log.info("Published SURVEY_COMPLETED event for user {} to partition {}", 
                            userId, result.getRecordMetadata().partition());
                    }
                });
        } catch (Exception e) {
            log.error("Error publishing SURVEY_COMPLETED event for user {}", userId, e);
        }
    }
    
    /**
     * Publish PROFILE_UPDATED event when behavior profile changes.
     */
    public void publishProfileUpdated(Long userId, String segment) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PROFILE_UPDATED");
        event.put("userId", userId);
        event.put("segment", segment);
        event.put("timestamp", Instant.now().toString());
        event.put("version", "1.0");
        
        try {
            kafkaTemplate.send(SURVEY_EVENTS_TOPIC, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PROFILE_UPDATED event for user {}", userId, ex);
                    } else {
                        log.info("Published PROFILE_UPDATED event for user {}", userId);
                    }
                });
        } catch (Exception e) {
            log.error("Error publishing PROFILE_UPDATED event for user {}", userId, e);
        }
    }
}
