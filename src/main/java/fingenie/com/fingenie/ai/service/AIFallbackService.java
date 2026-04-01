package fingenie.com.fingenie.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fingenie.com.fingenie.ai.entity.PredictionAuditLog;
import fingenie.com.fingenie.ai.repository.PredictionAuditLogRepository;
import fingenie.com.fingenie.entity.SpendingGuess;
import fingenie.com.fingenie.repository.SpendingGuessRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI Fallback Service with Circuit Breaker pattern.
 */
@Slf4j
@Service
public class AIFallbackService {

    private static final String CACHE_PREFIX = "ai:prediction:";
    private static final String CIRCUIT_BREAKER_NAME = "aiService";

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SpendingGuessRepository spendingGuessRepository;
    private final PredictionAuditLogRepository auditLogRepository;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.enabled:true}")
    private boolean aiServiceEnabled;

    @Value("${ai.fallback.enabled:true}")
    private boolean fallbackEnabled;

    @Value("${ai.fallback.cache-ttl-hours:24}")
    private int cacheTtlHours;

    @Value("${ai.model.version:v1.0.0}")
    private String modelVersion;

    @Value("${ai.model.min-confidence:0.5}")
    private double minConfidence;

    public AIFallbackService(
            RestTemplate restTemplate,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            SpendingGuessRepository spendingGuessRepository,
            PredictionAuditLogRepository auditLogRepository
    ) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.spendingGuessRepository = spendingGuessRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackPrediction")
    @TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @RateLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<PredictionResult> getPrediction(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            log.info("Requesting AI prediction for user {} with correlationId {}", userId, correlationId);

            if (!aiServiceEnabled) {
                log.debug("AI service disabled, using fallback");
                throw new AIServiceDisabledException("AI service is disabled");
            }

            try {
                String url = aiServiceUrl + "/predict";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> request = new HashMap<>();
                request.put("user_id", userId);
                request.put("correlation_id", correlationId);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

                JsonNode response = restTemplate.postForObject(url, entity, JsonNode.class);

                PredictionResult result = parsePredictionResponse(response, correlationId);

                cachePrediction(userId, result);
                logPredictionAudit(userId, result, false);

                return result;

            } catch (Exception e) {
                log.error("AI service call failed for user {}: {}", userId, e.getMessage());
                throw new AIServiceException("Failed to get prediction from AI service", e);
            }
        });
    }

    public CompletableFuture<PredictionResult> fallbackPrediction(Long userId, Throwable throwable) {
        log.warn("AI service fallback triggered for user {} due to: {}", userId, throwable.getMessage());

        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();

            PredictionResult result = null;
            String fallbackReason = throwable.getMessage();

            if (fallbackEnabled) {
                result = tryRuleBasedPrediction(userId, correlationId);

                if (result == null || result.getConfidence() < minConfidence) {
                    PredictionResult cached = getCachedPrediction(userId);
                    if (cached != null && cached.getConfidence() >= minConfidence) {
                        result = cached;
                        result.setFallbackUsed(true);
                        result.setFallbackReason("Using cached prediction");
                    }
                }
            }

            if (result == null) {
                result = getSafeDefault(userId, correlationId, fallbackReason);
            }

            logPredictionAudit(userId, result, true);

            return result;
        });
    }

    private PredictionResult tryRuleBasedPrediction(Long userId, String correlationId) {
        try {
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            var recentGuesses = spendingGuessRepository.findRecentCompletedGuesses(userId, weekAgo);

            if (recentGuesses.isEmpty()) {
                log.debug("No recent guesses for user {}, cannot use rule-based prediction", userId);
                return null;
            }

            BigDecimal totalActual = recentGuesses.stream()
                    .map(SpendingGuess::getFinalAmount)
                    .filter(a -> a != null && a.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long validCount = recentGuesses.stream()
                    .filter(g -> g.getFinalAmount() != null && g.getFinalAmount().compareTo(BigDecimal.ZERO) > 0)
                    .count();

            if (validCount == 0) {
                return null;
            }

            BigDecimal avgDaily = totalActual.divide(BigDecimal.valueOf(validCount), 2, RoundingMode.HALF_UP);

            double mean = avgDaily.doubleValue();
            double variance = recentGuesses.stream()
                    .filter(g -> g.getFinalAmount() != null)
                    .mapToDouble(g -> Math.pow(g.getFinalAmount().doubleValue() - mean, 2))
                    .average()
                    .orElse(0);
            double volatility = Math.sqrt(variance) / (mean + 1);

            double confidence = Math.max(0.3, 0.7 - volatility);

            return PredictionResult.builder()
                    .userId(userId)
                    .predictedAmount(avgDaily)
                    .confidence(confidence)
                    .riskScore(Math.min(1.0, volatility))
                    .modelVersion("rule-based-v1")
                    .correlationId(correlationId)
                    .fallbackUsed(true)
                    .fallbackReason("Rule-based prediction")
                    .explanation(Map.of(
                            "method", "7-day rolling average",
                            "sample_size", validCount,
                            "volatility", volatility
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Rule-based prediction failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private PredictionResult getCachedPrediction(Long userId) {
        try {
            String cacheKey = CACHE_PREFIX + userId;
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                if (cached instanceof PredictionResult) {
                    return (PredictionResult) cached;
                }
                return objectMapper.convertValue(cached, PredictionResult.class);
            }
        } catch (Exception e) {
            log.warn("Failed to get cached prediction for user {}: {}", userId, e.getMessage());
        }
        return null;
    }

    private void cachePrediction(Long userId, PredictionResult result) {
        try {
            String cacheKey = CACHE_PREFIX + userId;
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofHours(cacheTtlHours));
        } catch (Exception e) {
            log.warn("Failed to cache prediction for user {}: {}", userId, e.getMessage());
        }
    }

    private PredictionResult getSafeDefault(Long userId, String correlationId, String reason) {
        log.info("Using safe default prediction for user {}", userId);

        return PredictionResult.builder()
                .userId(userId)
                .predictedAmount(BigDecimal.valueOf(500000))
                .confidence(0.3)
                .riskScore(0.5)
                .modelVersion("safe-default-v1")
                .correlationId(correlationId)
                .fallbackUsed(true)
                .fallbackReason("Safe default: " + reason)
                .explanation(Map.of(
                        "method", "safe_default",
                        "reason", reason
                ))
                .build();
    }

    private PredictionResult parsePredictionResponse(JsonNode response, String correlationId) {
        return PredictionResult.builder()
                .userId(response.path("user_id").asLong())
                .predictedAmount(BigDecimal.valueOf(response.path("predicted_amount").asDouble()))
                .predictedCategory(response.path("predicted_category").asText(null))
                .confidence(response.path("confidence").asDouble())
                .riskScore(response.path("risk_score").asDouble())
                .modelVersion(response.path("model_version").asText(modelVersion))
                .featureHash(response.path("feature_hash").asText())
                .correlationId(correlationId)
                .inferenceLatencyMs(response.path("inference_latency_ms").asInt())
                .fallbackUsed(response.path("fallback_used").asBoolean(false))
                .fallbackReason(response.path("fallback_reason").asText(null))
                .explanation(parseExplanation(response.path("explanation")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseExplanation(JsonNode explanationNode) {
        try {
            return objectMapper.convertValue(explanationNode, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void logPredictionAudit(Long userId, PredictionResult result, boolean fallbackUsed) {
        try {
            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("predicted_amount", result.getPredictedAmount());
            responsePayload.put("predicted_category", result.getPredictedCategory());
            responsePayload.put("confidence", result.getConfidence());
            responsePayload.put("risk_score", result.getRiskScore());

            PredictionAuditLog audit = new PredictionAuditLog();
            audit.setUserId(userId);
            audit.setPredictionType("SPENDING_GUESS");
            audit.setModelVersion(result.getModelVersion() != null ? result.getModelVersion() : modelVersion);
            audit.setFeatureSnapshotHash(result.getFeatureHash() != null ? result.getFeatureHash() : "unavailable");
            audit.setExplanationVector(result.getExplanation() != null ? result.getExplanation() : Map.of());
            audit.setRawScore(BigDecimal.valueOf(result.getRiskScore()).setScale(6, RoundingMode.HALF_UP));
            audit.setConfidenceScore(BigDecimal.valueOf(result.getConfidence()).setScale(4, RoundingMode.HALF_UP));
            audit.setInferenceLatencyMs(result.getInferenceLatencyMs() != null ? result.getInferenceLatencyMs() : 0);
            audit.setFallbackUsed(fallbackUsed);
            audit.setFallbackReason(result.getFallbackReason());
            audit.setResponsePayload(responsePayload);
            audit.setCorrelationId(result.getCorrelationId());

            auditLogRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to log prediction audit: {}", e.getMessage());
        }
    }

    public static class AIServiceException extends RuntimeException {
        public AIServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AIServiceDisabledException extends RuntimeException {
        public AIServiceDisabledException(String message) {
            super(message);
        }
    }
}
