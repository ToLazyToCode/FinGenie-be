package fingenie.com.fingenie.kafka.consumer;

import fingenie.com.fingenie.ai.entity.PredictionAuditLog;
import fingenie.com.fingenie.ai.repository.PredictionAuditLogRepository;
import fingenie.com.fingenie.config.KafkaConfig;
import fingenie.com.fingenie.entity.SpendingGuess;
import fingenie.com.fingenie.kafka.event.ModelPredictionEvent;
import fingenie.com.fingenie.repository.SpendingGuessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelPredictionConsumer {

    private final SpendingGuessRepository spendingGuessRepository;
    private final PredictionAuditLogRepository predictionAuditLogRepository;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_MODEL_PREDICTIONS,
            groupId = "fingenie-prediction-consumers",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumePrediction(
            @Payload ModelPredictionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received prediction event: userId={}, predictionId={}, modelVersion={}, partition={}, offset={}",
                event.getUserId(), event.getPredictionId(), event.getModelVersion(), partition, offset);

        try {
            if (isDuplicate(event)) {
                log.info("Skipping duplicate prediction event: correlationId={}, predictionId={}, userId={}",
                        event.getCorrelationId(), event.getPredictionId(), event.getUserId());
                ack.acknowledge();
                return;
            }

            storePredictionAudit(event);

            if (event.getPredictionId() != null || event.getTransactionId() != null) {
                updateSpendingGuess(event);
            }

            ack.acknowledge();

            log.debug("Successfully processed prediction event: correlationId={}",
                    event.getCorrelationId());

        } catch (Exception e) {
            log.error("Error processing prediction event: correlationId={}, error={}",
                    event.getCorrelationId(), e.getMessage(), e);
            throw e;
        }
    }

    private boolean isDuplicate(ModelPredictionEvent event) {
        if (event.getCorrelationId() == null || event.getUserId() == null) {
            return false;
        }

        return predictionAuditLogRepository.existsByCorrelationIdAndPredictionIdAndUserId(
                event.getCorrelationId(),
                event.getPredictionId(),
                event.getUserId()
        );
    }

    private void storePredictionAudit(ModelPredictionEvent event) {
        Map<String, Object> explanationVector = new HashMap<>();
        if (event.getExplanation() != null) {
            explanationVector.put("risk_score", event.getExplanation().getRiskScore());
            explanationVector.put("factors", event.getExplanation().getFactors());
            explanationVector.put("summary", event.getExplanation().getSummary());
        }

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("predicted_amount", event.getPredictedAmount());
        responsePayload.put("predicted_category", event.getPredictedCategory());
        responsePayload.put("confidence", event.getConfidence());

        PredictionAuditLog audit = PredictionAuditLog.builder()
                .userId(event.getUserId())
                .predictionId(event.getPredictionId())
                .predictionType(event.getPredictionType())
                .modelVersion(event.getModelVersion())
                .featureSnapshotHash(event.getFeatureHash())
                .explanationVector(explanationVector)
                .rawScore(event.getRawScore())
                .confidenceScore(event.getConfidence())
                .inferenceLatencyMs(event.getInferenceLatencyMs())
                .fallbackUsed(event.getFallbackUsed())
                .fallbackReason(event.getFallbackReason())
                .responsePayload(responsePayload)
                .errorMessage(event.getErrorMessage())
                .correlationId(event.getCorrelationId())
                .build();

        audit.setCreatedAt(Timestamp.from(Instant.now()));
        predictionAuditLogRepository.save(audit);

        log.debug("Stored prediction audit: userId={}, modelVersion={}",
                event.getUserId(), event.getModelVersion());
    }

    private void updateSpendingGuess(ModelPredictionEvent event) {
        Optional<SpendingGuess> guessOpt = Optional.empty();

        if (event.getPredictionId() != null) {
            guessOpt = spendingGuessRepository.findById(event.getPredictionId());
        }

        if (guessOpt.isEmpty() && event.getTransactionId() != null) {
            guessOpt = spendingGuessRepository.findFirstByCreatedTransactionId(event.getTransactionId());
        }

        if (guessOpt.isEmpty()) {
            log.warn("SpendingGuess not found for predictionId={}, transactionId={}, userId={}",
                    event.getPredictionId(), event.getTransactionId(), event.getUserId());
            return;
        }

        SpendingGuess guess = guessOpt.get();

        if (event.getPredictedAmount() != null) {
            guess.setGuessedAmount(event.getPredictedAmount());
        }
        if (event.getConfidence() != null) {
            guess.setConfidence(event.getConfidence());
        }

        if (event.getExplanation() != null && event.getExplanation().getSummary() != null) {
            guess.setReasoning(event.getExplanation().getSummary());
        }

        spendingGuessRepository.save(guess);

        log.debug("Updated SpendingGuess: id={}, amount={}, confidence={}",
                guess.getId(), guess.getGuessedAmount(), guess.getConfidence());
    }
}
