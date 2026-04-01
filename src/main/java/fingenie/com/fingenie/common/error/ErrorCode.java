package fingenie.com.fingenie.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Centralized Error Code Registry
 * 
 * Enterprise-grade error code system providing:
 * - Consistent error identifiers across the application
 * - HTTP status mapping
 * - Severity classification for logging/monitoring
 * - i18n message key mapping
 * 
 * RULES:
 * - All custom exceptions MUST use an ErrorCode
 * - No raw string error messages allowed
 * - Error codes should be searchable in logs
 */
@Getter
public enum ErrorCode {

    // ============================================
    // AUTHENTICATION ERRORS (AUTH_*)
    // ============================================
    
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, Severity.WARN, false),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, Severity.INFO, false),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, Severity.WARN, false),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, Severity.WARN, false),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, Severity.INFO, false),
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, Severity.WARN, false),
    AUTH_SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, Severity.INFO, false),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, Severity.WARN, false),
    AUTH_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, Severity.WARN, false),
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, Severity.INFO, false),
    AUTH_OTP_INVALID(HttpStatus.BAD_REQUEST, Severity.WARN, false),
    AUTH_OTP_EXPIRED(HttpStatus.BAD_REQUEST, Severity.INFO, false),
    AUTH_OTP_MAX_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, Severity.WARN, false),

    // ============================================
    // USER ERRORS (USER_*)
    // ============================================
    
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, Severity.WARN, false),
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, Severity.INFO, false),
    USER_PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, Severity.INFO, false),
    USER_PROFILE_INCOMPLETE(HttpStatus.BAD_REQUEST, Severity.INFO, false),

    // ============================================
    // VALIDATION ERRORS (VALIDATION_*)
    // ============================================
    
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, Severity.INFO, false),
    VALIDATION_FIELD_REQUIRED(HttpStatus.BAD_REQUEST, Severity.INFO, false),
    VALIDATION_FIELD_INVALID(HttpStatus.BAD_REQUEST, Severity.INFO, false),
    VALIDATION_CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, Severity.INFO, false),

    // ============================================
    // WALLET ERRORS (WALLET_*)
    // ============================================
    
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, Severity.WARN, false),
    WALLET_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, Severity.INFO, false),
    WALLET_TRANSFER_FAILED(HttpStatus.BAD_REQUEST, Severity.ERROR, false),
    WALLET_ACCESS_DENIED(HttpStatus.FORBIDDEN, Severity.WARN, false),

    // ============================================
    // TRANSACTION ERRORS (TRANSACTION_*)
    // ============================================
    
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, Severity.WARN, false),
    TRANSACTION_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, Severity.INFO, false),
    TRANSACTION_DUPLICATE(HttpStatus.CONFLICT, Severity.WARN, false),

    // ============================================
    // BUDGET ERRORS (BUDGET_*)
    // ============================================
    
    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, Severity.WARN, false),
    BUDGET_EXCEEDED(HttpStatus.BAD_REQUEST, Severity.INFO, false),
    BUDGET_INVALID_PERIOD(HttpStatus.BAD_REQUEST, Severity.INFO, false),

    // ============================================
    // PIGGY GOAL ERRORS (PIGGY_*)
    // ============================================
    
    PIGGY_GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, Severity.WARN, false),
    PIGGY_GOAL_COMPLETED(HttpStatus.BAD_REQUEST, Severity.INFO, false),

    // ============================================
    // PET ERRORS (PET_*)
    // ============================================
    
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, Severity.WARN, false),
    PET_ALREADY_EXISTS(HttpStatus.CONFLICT, Severity.INFO, false),

    // ============================================
    // AI ERRORS (AI_*)
    // ============================================
    
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, Severity.ERROR, true),
    AI_RESPONSE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, Severity.ERROR, true),
    AI_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, Severity.WARN, false),

    // ============================================
    // NOTIFICATION ERRORS (NOTIFICATION_*)
    // ============================================
    
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, Severity.WARN, false),
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, Severity.ERROR, true),

    // ============================================
    // EXTERNAL SERVICE ERRORS (EXTERNAL_*)
    // ============================================
    
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, Severity.ERROR, true),
    EXTERNAL_SERVICE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, Severity.ERROR, true),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, Severity.ERROR, true),

    // ============================================
    // RATE LIMITING ERRORS (RATE_*)
    // ============================================
    
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, Severity.WARN, false),

    // ============================================
    // DATABASE ERRORS (DATABASE_*)
    // ============================================
    
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, Severity.ERROR, true),
    DATABASE_CONSTRAINT_VIOLATION(HttpStatus.CONFLICT, Severity.WARN, false),

    // ============================================
    // SYSTEM ERRORS (SYSTEM_*)
    // ============================================
    
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, Severity.ERROR, true),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, Severity.ERROR, true),
    CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, Severity.ERROR, true);

    // ============================================
    // FIELDS
    // ============================================
    
    private final HttpStatus httpStatus;
    private final Severity severity;
    private final boolean reportToSentry;

    ErrorCode(HttpStatus httpStatus, Severity severity, boolean reportToSentry) {
        this.httpStatus = httpStatus;
        this.severity = severity;
        this.reportToSentry = reportToSentry;
    }

    /**
     * Get the i18n message key for this error code
     */
    public String getMessageKey() {
        return this.name();
    }

    /**
     * Check if this error should be logged as an error (vs warn/info)
     */
    public boolean isServerError() {
        return httpStatus.is5xxServerError();
    }

    /**
     * Check if this is a client error (4xx)
     */
    public boolean isClientError() {
        return httpStatus.is4xxClientError();
    }

    /**
     * Get default message for this error code (fallback when i18n not available)
     */
    public String getDefaultMessage() {
        return this.name().replace("_", " ").toLowerCase();
    }

    /**
     * Severity levels for logging and monitoring
     */
    public enum Severity {
        /** Debug level - not normally logged in production */
        DEBUG,
        /** Informational - expected business events */
        INFO,
        /** Warning - unexpected but handled situations */
        WARN,
        /** Error - requires attention, may need investigation */
        ERROR,
        /** Critical - system failure, immediate action required */
        CRITICAL
    }
}
