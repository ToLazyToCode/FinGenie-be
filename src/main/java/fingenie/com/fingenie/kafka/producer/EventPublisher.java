package fingenie.com.fingenie.kafka.producer;

import fingenie.com.fingenie.config.KafkaConfig;
import fingenie.com.fingenie.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * EventPublisher - Publishes domain events to Kafka topics.
 *
 * Responsibilities:
 * - Publish transaction events when transactions are created/updated/deleted
 * - Publish user events for activity tracking
 * - Publish survey events for AI learning
 * - Publish feature update notifications
 *
 * All events include:
 * - Idempotency keys for deduplication
 * - Correlation IDs for distributed tracing
 * - Schema versioning for evolution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ============================================
    // Transaction Events
    // ============================================

    public CompletableFuture<SendResult<String, Object>> publishTransactionCreated(
            TransactionEvent event) {
        return publish(KafkaConfig.TOPIC_TRANSACTION_EVENTS,
                event.getPayload().getUserId().toString(), event, "TRANSACTION_CREATED");
    }

    public CompletableFuture<SendResult<String, Object>> publishTransactionUpdated(
            TransactionEvent event) {
        return publish(KafkaConfig.TOPIC_TRANSACTION_EVENTS,
                event.getPayload().getUserId().toString(), event, "TRANSACTION_UPDATED");
    }

    public CompletableFuture<SendResult<String, Object>> publishTransactionDeleted(
            TransactionEvent event) {
        return publish(KafkaConfig.TOPIC_TRANSACTION_EVENTS,
                event.getPayload().getUserId().toString(), event, "TRANSACTION_DELETED");
    }

    // ============================================
    // User Events
    // ============================================

    public CompletableFuture<SendResult<String, Object>> publishUserEvent(UserEvent event) {
        return publish(KafkaConfig.TOPIC_USER_EVENTS,
                event.getUserId().toString(), event, event.getEventType());
    }

    public CompletableFuture<SendResult<String, Object>> publishUserLogin(
            Long userId, Long accountId, String deviceId, String ipAddress) {
        UserEvent event = UserEvent.login(userId, accountId, deviceId, ipAddress);
        return publishUserEvent(event);
    }

    public CompletableFuture<SendResult<String, Object>> publishUserLogout(
            Long userId, Long accountId) {
        UserEvent event = UserEvent.logout(userId, accountId);
        return publishUserEvent(event);
    }

    // ============================================
    // Survey Events
    // ============================================

    public CompletableFuture<SendResult<String, Object>> publishSurveyEvent(SurveyEvent event) {
        return publish(KafkaConfig.TOPIC_SURVEY_EVENTS,
                event.getUserId().toString(), event, event.getEventType());
    }

    public CompletableFuture<SendResult<String, Object>> publishPredictionFeedback(
            Long userId, Long predictionId, String feedbackType,
            java.math.BigDecimal originalAmount, java.math.BigDecimal correctedAmount,
            String reason) {
        SurveyEvent event = SurveyEvent.predictionFeedback(
                userId, predictionId, feedbackType, originalAmount, correctedAmount, reason);
        return publishSurveyEvent(event);
    }

    public CompletableFuture<SendResult<String, Object>> publishEmotionalCheckIn(
            Long userId, java.math.BigDecimal emotionalScore, String mood, String trigger) {
        SurveyEvent event = SurveyEvent.emotionalCheckIn(userId, emotionalScore, mood, trigger);
        return publishSurveyEvent(event);
    }

    // ============================================
    // Feature Update Events
    // ============================================

    public CompletableFuture<SendResult<String, Object>> publishFeatureUpdate(
            FeatureUpdateEvent event) {
        return publish(KafkaConfig.TOPIC_FEATURE_UPDATES,
                event.getUserId().toString(), event, "FEATURE_UPDATE");
    }

    public CompletableFuture<SendResult<String, Object>> publishFeatureUpdateFromTransaction(
            Long userId, String featureHash, String transactionEventId) {
        FeatureUpdateEvent event = FeatureUpdateEvent.fromTransaction(
                userId, featureHash, transactionEventId);
        return publishFeatureUpdate(event);
    }

    // ============================================
    // Internal Publish Method
    // ============================================

    private CompletableFuture<SendResult<String, Object>> publish(
            String topic, String key, Object event, String eventType) {

        log.debug("Publishing {} to topic {} with key {}", eventType, topic, key);

        if (key == null) {
            log.error("Skipping publish for {} because message key is null", eventType);
            return CompletableFuture.failedFuture(new IllegalArgumentException("Kafka key cannot be null"));
        }

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} to topic {}: {}",
                        eventType, topic, ex.getMessage(), ex);
            } else {
                log.debug("Successfully published {} to topic {} partition {} offset {}",
                        eventType, topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }
}
