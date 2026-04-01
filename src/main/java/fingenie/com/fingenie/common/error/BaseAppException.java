package fingenie.com.fingenie.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Base Application Exception
 * 
 * All custom application exceptions MUST extend this class.
 * This ensures consistent error handling across the application.
 * 
 * Features:
 * - Enforces use of ErrorCode enum
 * - Supports additional context data
 * - Provides HTTP status mapping
 * - Integrates with i18n system
 * 
 * RULES:
 * - Never throw RuntimeException directly
 * - Never use raw string error messages
 * - Always provide ErrorCode
 */
@Getter
public abstract class BaseAppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    /**
     * Create exception with ErrorCode only
     */
    protected BaseAppException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.context = Collections.emptyMap();
    }

    /**
     * Create exception with ErrorCode and cause
     */
    protected BaseAppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.name(), cause);
        this.errorCode = errorCode;
        this.context = Collections.emptyMap();
    }

    /**
     * Create exception with ErrorCode and context data
     * Context data can be used for i18n message interpolation
     */
    protected BaseAppException(ErrorCode errorCode, Map<String, Object> context) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.context = context != null ? Collections.unmodifiableMap(context) : Collections.emptyMap();
    }

    /**
     * Create exception with ErrorCode, context, and cause
     */
    protected BaseAppException(ErrorCode errorCode, Map<String, Object> context, Throwable cause) {
        super(errorCode.name(), cause);
        this.errorCode = errorCode;
        this.context = context != null ? Collections.unmodifiableMap(context) : Collections.emptyMap();
    }

    /**
     * Get HTTP status from ErrorCode
     */
    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    /**
     * Get error code string (enum name)
     */
    public String getCode() {
        return errorCode.name();
    }

    /**
     * Get i18n message key
     */
    public String getMessageKey() {
        return errorCode.getMessageKey();
    }

    /**
     * Check if this error should be reported to Sentry
     */
    public boolean shouldReportToSentry() {
        return errorCode.isReportToSentry();
    }

    /**
     * Check if this is a server error (5xx)
     */
    public boolean isServerError() {
        return errorCode.isServerError();
    }

    /**
     * Get severity for logging
     */
    public ErrorCode.Severity getSeverity() {
        return errorCode.getSeverity();
    }

    /**
     * Get message arguments for i18n interpolation
     * Extracts values from context map for use in message templates
     */
    public Object[] getMessageArgs() {
        if (context == null || context.isEmpty()) {
            return new Object[0];
        }
        return context.values().toArray();
    }
}
