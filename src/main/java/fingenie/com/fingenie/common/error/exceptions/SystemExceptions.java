package fingenie.com.fingenie.common.error.exceptions;

import fingenie.com.fingenie.common.error.BaseAppException;
import fingenie.com.fingenie.common.error.ErrorCode;

import java.util.Map;

/**
 * System-level Exceptions
 */
public class SystemExceptions {

    private SystemExceptions() {
        // Utility class
    }

    // ============================================
    // VALIDATION EXCEPTIONS
    // ============================================

    public static class ValidationException extends BaseAppException {
        public ValidationException() {
            super(ErrorCode.VALIDATION_FAILED);
        }

        public ValidationException(Map<String, Object> fieldErrors) {
            super(ErrorCode.VALIDATION_FAILED, fieldErrors);
        }
    }

    // ============================================
    // DATABASE EXCEPTIONS
    // ============================================

    public static class DatabaseException extends BaseAppException {
        public DatabaseException(Throwable cause) {
            super(ErrorCode.DATABASE_ERROR, cause);
        }
    }

    public static class DatabaseConstraintViolationException extends BaseAppException {
        public DatabaseConstraintViolationException(String constraintName) {
            super(ErrorCode.DATABASE_CONSTRAINT_VIOLATION, Map.of("constraint", constraintName));
        }
    }

    // ============================================
    // EXTERNAL SERVICE EXCEPTIONS
    // ============================================

    public static class ExternalServiceUnavailableException extends BaseAppException {
        public ExternalServiceUnavailableException(String serviceName) {
            super(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, Map.of("service", serviceName));
        }

        public ExternalServiceUnavailableException(String serviceName, Throwable cause) {
            super(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, Map.of("service", serviceName), cause);
        }
    }

    public static class ExternalServiceTimeoutException extends BaseAppException {
        public ExternalServiceTimeoutException(String serviceName, long timeoutMs) {
            super(ErrorCode.EXTERNAL_SERVICE_TIMEOUT, Map.of(
                "service", serviceName,
                "timeoutMs", timeoutMs
            ));
        }
    }

    public static class ExternalServiceErrorException extends BaseAppException {
        public ExternalServiceErrorException(String serviceName, String errorMessage) {
            super(ErrorCode.EXTERNAL_SERVICE_ERROR, Map.of(
                "service", serviceName,
                "error", errorMessage
            ));
        }
    }

    // ============================================
    // AI SERVICE EXCEPTIONS
    // ============================================

    public static class AIServiceUnavailableException extends BaseAppException {
        public AIServiceUnavailableException() {
            super(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        public AIServiceUnavailableException(Throwable cause) {
            super(ErrorCode.AI_SERVICE_UNAVAILABLE, cause);
        }
    }

    public static class AIResponseInvalidException extends BaseAppException {
        public AIResponseInvalidException(String reason) {
            super(ErrorCode.AI_RESPONSE_INVALID, Map.of("reason", reason));
        }
    }

    public static class AIRateLimitedException extends BaseAppException {
        public AIRateLimitedException() {
            super(ErrorCode.AI_RATE_LIMITED);
        }
    }

    // ============================================
    // RATE LIMITING EXCEPTIONS
    // ============================================

    public static class RateLimitExceededException extends BaseAppException {
        public RateLimitExceededException() {
            super(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        public RateLimitExceededException(String endpoint, int limit, int windowSeconds) {
            super(ErrorCode.RATE_LIMIT_EXCEEDED, Map.of(
                "endpoint", endpoint,
                "limit", limit,
                "windowSeconds", windowSeconds
            ));
        }
    }

    // ============================================
    // INTERNAL ERRORS
    // ============================================

    public static class InternalServerException extends BaseAppException {
        public InternalServerException() {
            super(ErrorCode.INTERNAL_ERROR);
        }

        public InternalServerException(Throwable cause) {
            super(ErrorCode.INTERNAL_ERROR, cause);
        }

        public InternalServerException(String context) {
            super(ErrorCode.INTERNAL_ERROR, Map.of("context", context));
        }
    }

    public static class ServiceUnavailableException extends BaseAppException {
        public ServiceUnavailableException(String reason) {
            super(ErrorCode.SERVICE_UNAVAILABLE, Map.of("reason", reason));
        }
    }

    public static class ConfigurationException extends BaseAppException {
        public ConfigurationException(String configKey) {
            super(ErrorCode.CONFIGURATION_ERROR, Map.of("configKey", configKey));
        }
    }

    // ============================================
    // NOTIFICATION EXCEPTIONS
    // ============================================

    public static class NotificationSendFailedException extends BaseAppException {
        public NotificationSendFailedException(String channel, Throwable cause) {
            super(ErrorCode.NOTIFICATION_SEND_FAILED, Map.of("channel", channel), cause);
        }
    }
}
