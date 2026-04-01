package fingenie.com.fingenie.ai.client;

import fingenie.com.fingenie.ai.client.dto.ChatRequest;
import fingenie.com.fingenie.ai.client.dto.InsightRequest;
import fingenie.com.fingenie.ai.client.dto.PredictionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final String INSIGHT_ENDPOINT = "/ai/insight";

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;

    @Value("${ai.client.timeout-seconds:8}")
    private long defaultTimeoutSeconds;

    @Value("${ai.client.timeout-seconds.chat:${ai.client.timeout-seconds:8}}")
    private long chatTimeoutSeconds;

    @Value("${ai.client.timeout-seconds.saving-plan-advice:${ai.client.timeout-seconds:8}}")
    private long savingPlanAdviceTimeoutSeconds;

    @Value("${ai.client.timeout-seconds.insight:${ai.client.timeout-seconds:8}}")
    private long insightTimeoutSeconds;

    @Value("${ai.client.timeout-seconds.predict:${ai.client.timeout-seconds:8}}")
    private long predictTimeoutSeconds;

    @Value("${ai.client.timeout-seconds.health:3}")
    private long healthTimeoutSeconds;

    @Value("${ai.client.retry-attempts:2}")
    private long retryAttempts;

    public Map<String, Object> chat(ChatRequest request) {
        return post("/ai/chat", request, Duration.ofSeconds(Math.max(chatTimeoutSeconds, 1)));
    }

    public Map<String, Object> predict(PredictionRequest request) {
        return post("/ai/predict", request, Duration.ofSeconds(Math.max(predictTimeoutSeconds, 1)));
    }

    public Map<String, Object> insight(InsightRequest request) {
        return post(INSIGHT_ENDPOINT, request, Duration.ofSeconds(Math.max(insightTimeoutSeconds, 1)));
    }

    public Map<String, Object> savingPlanAdvice(Map<String, Object> payload) {
        return post(
                "/ai/saving-plan/advice",
                payload,
                Duration.ofSeconds(Math.max(savingPlanAdviceTimeoutSeconds, 1))
        );
    }

    public Map<String, Object> health() {
        return post("/health", Map.of(), Duration.ofSeconds(Math.max(healthTimeoutSeconds, 1)));
    }

    private Map<String, Object> post(String path, Object payload) {
        return post(path, payload, Duration.ofSeconds(Math.max(defaultTimeoutSeconds, 1)));
    }

    private Map<String, Object> post(String path, Object payload, Duration timeout) {
        long startedAtNanos = System.nanoTime();
        long timeoutMs = timeout.toMillis();
        try {
            Mono<Map<String, Object>> call = aiWebClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(MAP_TYPE);
                        }
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new AIClientException(
                                        path,
                                        AIClientException.FailureType.HTTP_ERROR,
                                        "AI service returned HTTP " + response.statusCode().value() + " on " + path,
                                        0L,
                                        timeoutMs,
                                        response.statusCode().value(),
                                        abbreviate(body),
                                        null
                                )));
                    })
                    .timeout(timeout);

            if (retryAttempts > 0) {
                call = call.retryWhen(
                        Retry.fixedDelay(retryAttempts, Duration.ofMillis(200))
                                .filter(this::isRetryable)
                );
            }

            Map<String, Object> response = call.block();
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
            if (response == null || response.isEmpty()) {
                throw new AIClientException(
                        path,
                        AIClientException.FailureType.EMPTY_RESPONSE,
                        "AI service returned empty response on " + path,
                        elapsedMs,
                        timeoutMs,
                        null,
                        null,
                        null
                );
            }
            return response;
        } catch (Exception ex) {
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
            AIClientException mapped = mapFailure(path, ex, elapsedMs, timeoutMs);
            if (mapped.getFailureType() == AIClientException.FailureType.TIMEOUT) {
                log.warn(
                        "AI service call failed path={} type={} elapsedMs={} timeoutMs={} statusCode={} providerMessage={} cause={}",
                        mapped.getPath(),
                        mapped.getFailureType(),
                        mapped.getElapsedMs(),
                        mapped.getTimeoutMs(),
                        mapped.getStatusCode(),
                        mapped.getProviderMessage(),
                        mapped.getMessage()
                );
            } else {
                log.error(
                        "AI service call failed path={} type={} elapsedMs={} timeoutMs={} statusCode={} providerMessage={}",
                        mapped.getPath(),
                        mapped.getFailureType(),
                        mapped.getElapsedMs(),
                        mapped.getTimeoutMs(),
                        mapped.getStatusCode(),
                        mapped.getProviderMessage(),
                        ex
                );
            }
            throw mapped;
        }
    }

    private AIClientException mapFailure(String path, Throwable throwable, long elapsedMs, long timeoutMs) {
        if (throwable instanceof AIClientException ex) {
            return ex.withElapsedMs(elapsedMs);
        }

        Throwable root = unwrap(throwable);
        if (root instanceof TimeoutException || root instanceof java.util.concurrent.TimeoutException) {
            return new AIClientException(
                    path,
                    AIClientException.FailureType.TIMEOUT,
                    "AI service timeout on " + path,
                    elapsedMs,
                    timeoutMs,
                    null,
                    abbreviate(root.getMessage()),
                    throwable
            );
        }

        if (root instanceof ConnectException) {
            return new AIClientException(
                    path,
                    AIClientException.FailureType.CONNECT_ERROR,
                    "AI service connection failed on " + path,
                    elapsedMs,
                    timeoutMs,
                    null,
                    abbreviate(root.getMessage()),
                    throwable
            );
        }

        if (root instanceof org.springframework.web.reactive.function.client.WebClientResponseException webEx) {
            return new AIClientException(
                    path,
                    AIClientException.FailureType.HTTP_ERROR,
                    "AI service HTTP error on " + path,
                    elapsedMs,
                    timeoutMs,
                    webEx.getRawStatusCode(),
                    abbreviate(webEx.getResponseBodyAsString()),
                    throwable
            );
        }

        if (root instanceof org.springframework.core.codec.DecodingException
                || root instanceof IllegalArgumentException) {
            return new AIClientException(
                    path,
                    AIClientException.FailureType.PARSE_ERROR,
                    "AI service response parse error on " + path,
                    elapsedMs,
                    timeoutMs,
                    null,
                    abbreviate(root.getMessage()),
                    throwable
            );
        }

        return new AIClientException(
                path,
                AIClientException.FailureType.UNKNOWN_ERROR,
                "AI service call failed on " + path,
                elapsedMs,
                timeoutMs,
                null,
                abbreviate(root == null ? throwable.getMessage() : root.getMessage()),
                throwable
        );
    }

    private boolean isRetryable(Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof AIClientException aiClientException) {
            return aiClientException.getFailureType() == AIClientException.FailureType.TIMEOUT
                    || aiClientException.getFailureType() == AIClientException.FailureType.CONNECT_ERROR;
        }
        return root instanceof TimeoutException || root instanceof ConnectException;
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable == null || throwable.getCause() == null) {
            return throwable;
        }
        return unwrap(throwable.getCause());
    }

    private static String abbreviate(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= 280) {
            return trimmed;
        }
        return trimmed.substring(0, 277) + "...";
    }
}
