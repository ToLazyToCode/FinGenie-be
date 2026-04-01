package fingenie.com.fingenie.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Structured Error Response
 * 
 * Standard error response format for all API errors.
 * Designed for:
 * - Consistent client-side handling
 * - Easy debugging with correlationId
 * - i18n support with localized messages
 * - Structured field errors for validation
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * ISO 8601 timestamp when the error occurred
     */
    private final Instant timestamp;

    /**
     * HTTP status code (e.g., 401, 404, 500)
     */
    private final int status;

    /**
     * Machine-readable error code (e.g., AUTH_INVALID_CREDENTIALS)
     * Maps to ErrorCode enum
     */
    private final String errorCode;

    /**
     * Human-readable localized error message
     */
    private final String message;

    /**
     * Unique request correlation ID for tracing
     */
    private final String correlationId;

    /**
     * Request path that triggered the error
     */
    private final String path;

    /**
     * Additional context data (optional)
     */
    private final Map<String, Object> context;

    /**
     * Field-specific validation errors (optional)
     */
    private final Map<String, String> fieldErrors;

    /**
     * Stack trace (only in development mode, never in production)
     * Only included for 5xx errors in non-production environments
     */
    private final String trace;

    /**
     * Create a simple error response
     */
    public static ErrorResponse of(
            int status,
            String errorCode,
            String message,
            String correlationId,
            String path
    ) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .correlationId(correlationId)
                .path(path)
                .build();
    }

    /**
     * Create an error response with context
     */
    public static ErrorResponse withContext(
            int status,
            String errorCode,
            String message,
            String correlationId,
            String path,
            Map<String, Object> context
    ) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .correlationId(correlationId)
                .path(path)
                .context(context)
                .build();
    }
}
