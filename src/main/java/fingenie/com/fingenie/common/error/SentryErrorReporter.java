package fingenie.com.fingenie.common.error;

import fingenie.com.fingenie.common.filter.CorrelationIdFilter;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility for reporting errors to Sentry.
 * 
 * Features:
 * - Conditional reporting based on ErrorCode.reportToSentry
 * - Automatic context enrichment (correlationId, userId, errorCode)
 * - Graceful degradation when Sentry is not configured
 * - Severity mapping from ErrorCode to Sentry levels
 */
@Slf4j
@Component
public class SentryErrorReporter {

    private static final boolean SENTRY_AVAILABLE = isSentryAvailable();

    private static boolean isSentryAvailable() {
        try {
            Class.forName("io.sentry.Sentry");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Reports an exception to Sentry if the error code is marked as reportable.
     *
     * @param errorCode The error code associated with the exception
     * @param exception The exception to report
     */
    public void reportIfNeeded(ErrorCode errorCode, Throwable exception) {
        if (!SENTRY_AVAILABLE) {
            log.trace("Sentry not available on classpath");
            return;
        }

        if (errorCode == null || !errorCode.isReportToSentry()) {
            return;
        }

        try {
            reportToSentry(errorCode, exception, null);
        } catch (Exception e) {
            log.warn("Failed to report to Sentry: {}", e.getMessage());
        }
    }

    /**
     * Reports an exception to Sentry with additional context.
     *
     * @param errorCode The error code associated with the exception
     * @param exception The exception to report
     * @param additionalContext Additional context to attach to the error
     */
    public void reportIfNeeded(ErrorCode errorCode, Throwable exception, Map<String, Object> additionalContext) {
        if (!SENTRY_AVAILABLE) {
            return;
        }

        if (errorCode == null || !errorCode.isReportToSentry()) {
            return;
        }

        try {
            reportToSentry(errorCode, exception, additionalContext);
        } catch (Exception e) {
            log.warn("Failed to report to Sentry: {}", e.getMessage());
        }
    }

    /**
     * Reports any exception to Sentry regardless of error code settings.
     * Use this for unexpected/unhandled exceptions.
     *
     * @param exception The exception to report
     */
    public void reportUnexpected(Throwable exception) {
        if (!SENTRY_AVAILABLE) {
            return;
        }

        try {
            Sentry.configureScope(scope -> {
                scope.setLevel(SentryLevel.ERROR);
                
                // Add correlation ID
                String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
                if (correlationId != null) {
                    scope.setTag("correlationId", correlationId);
                }

                // Add user ID
                String userId = MDC.get("userId");
                if (userId != null) {
                    scope.setTag("userId", userId);
                }

                scope.setTag("type", "unexpected");
            });

            Sentry.captureException(exception);
            log.debug("Reported unexpected exception to Sentry");
        } catch (Exception e) {
            log.warn("Failed to report unexpected exception to Sentry: {}", e.getMessage());
        }
    }

    private void reportToSentry(ErrorCode errorCode, Throwable exception, Map<String, Object> additionalContext) {
        Sentry.configureScope(scope -> {
            // Set severity level based on ErrorCode severity
            scope.setLevel(mapSeverityToSentryLevel(errorCode.getSeverity()));

            // Add correlation ID from MDC
            String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
            if (correlationId != null) {
                scope.setTag("correlationId", correlationId);
            }

            // Add user ID from MDC
            String userId = MDC.get("userId");
            if (userId != null) {
                scope.setTag("userId", userId);
            }

            // Add error code information
            scope.setTag("errorCode", errorCode.name());
            scope.setTag("httpStatus", String.valueOf(errorCode.getHttpStatus().value()));
            scope.setTag("severity", errorCode.getSeverity().name());

            // Add request context from MDC
            String requestPath = MDC.get("requestPath");
            String requestMethod = MDC.get("requestMethod");
            if (requestPath != null) {
                scope.setTag("request.path", requestPath);
            }
            if (requestMethod != null) {
                scope.setTag("request.method", requestMethod);
            }

            // Add additional context as extra data
            if (additionalContext != null) {
                additionalContext.forEach((key, value) -> {
                    if (value != null && !isSensitiveKey(key)) {
                        scope.setExtra(key, value.toString());
                    }
                });
            }
        });

        Sentry.captureException(exception);
        log.debug("Reported {} to Sentry with correlationId: {}", 
                errorCode.name(), 
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    private SentryLevel mapSeverityToSentryLevel(ErrorCode.Severity severity) {
        return switch (severity) {
            case DEBUG, INFO -> SentryLevel.INFO;
            case WARN -> SentryLevel.WARNING;
            case ERROR -> SentryLevel.ERROR;
            case CRITICAL -> SentryLevel.FATAL;
        };
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("token") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("key") ||
               lowerKey.contains("authorization") ||
               lowerKey.contains("credit") ||
               lowerKey.contains("ssn") ||
               lowerKey.contains("pin");
    }

    /**
     * Checks if Sentry is available and configured.
     */
    public boolean isEnabled() {
        if (!SENTRY_AVAILABLE) {
            return false;
        }
        try {
            return Sentry.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
}
