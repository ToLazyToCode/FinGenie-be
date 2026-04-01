package fingenie.com.fingenie.common;

import fingenie.com.fingenie.common.error.BaseAppException;
import fingenie.com.fingenie.common.error.ErrorCode;
import fingenie.com.fingenie.common.error.ErrorResponse;
import fingenie.com.fingenie.common.error.SentryErrorReporter;
import fingenie.com.fingenie.common.filter.CorrelationIdFilter;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Enterprise-grade Global Exception Handler with:
 * - Centralized error code management via ErrorCode enum
 * - i18n support for error messages
 * - Correlation ID tracking for distributed tracing
 * - Structured logging with severity-based log levels
 * - Sentry integration for error reporting (5xx errors only)
 * - Context preservation for debugging
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;
    private final SentryErrorReporter sentryReporter;

    // ========================================
    // APPLICATION EXCEPTIONS (BaseAppException)
    // ========================================

    /**
     * Handles all application exceptions that extend BaseAppException.
     * These are expected business/domain exceptions with proper error codes.
     */
    @ExceptionHandler(BaseAppException.class)
    public ResponseEntity<ErrorResponse> handleBaseAppException(
            BaseAppException ex, 
            HttpServletRequest request) {
        
        ErrorCode errorCode = ex.getErrorCode();
        String correlationId = getCorrelationId();
        String message = resolveMessage(errorCode, ex.getMessageArgs());
        
        // Log with appropriate level based on severity
        logException(errorCode, ex, correlationId, request);
        
        // Report to Sentry if configured
        sentryReporter.reportIfNeeded(errorCode, ex, ex.getContext());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.name())
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .context(sanitizeContext(ex.getContext()))
                .build();
        
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    // ========================================
    // LEGACY EXCEPTION SUPPORT
    // ========================================

    /**
     * Handles legacy CustomException for backward compatibility.
     * @deprecated Migrate to BaseAppException subclasses
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            CustomException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        
        log.warn("[{}] Legacy CustomException: {} - {}", 
                correlationId, ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(ex.getStatus().value())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // ========================================
    // AUTHENTICATION & AUTHORIZATION
    // ========================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS;
        
        log.warn("[{}] Authentication failed: {}", correlationId, ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.AUTH_INVALID_CREDENTIALS;
        
        log.warn("[{}] Bad credentials attempt", correlationId);
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.AUTH_ACCESS_DENIED;
        
        log.warn("[{}] Access denied: {}", correlationId, request.getRequestURI());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ========================================
    // VALIDATION ERRORS
    // ========================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        log.info("[{}] Validation failed: {} field errors", correlationId, fieldErrors.size());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.VALIDATION_CONSTRAINT_VIOLATION;
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            fieldErrors.put(field, violation.getMessage());
        });
        
        log.info("[{}] Constraint violation: {}", correlationId, fieldErrors);
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.VALIDATION_FIELD_REQUIRED;
        
        Map<String, String> fieldErrors = Map.of(ex.getParameterName(), "Required parameter is missing");
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode.name())
                .message("Missing required parameter: " + ex.getParameterName())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.VALIDATION_FIELD_INVALID;
        
        String message = String.format("Invalid value '%s' for parameter '%s'", 
                ex.getValue(), ex.getName());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode.name())
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    // ========================================
    // REQUEST FORMAT ERRORS
    // ========================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        
        log.info("[{}] Malformed request body", correlationId);
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("MALFORMED_REQUEST")
                .message("Request body is malformed or missing")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .errorCode("UNSUPPORTED_MEDIA_TYPE")
                .message("Content type '" + ex.getContentType() + "' is not supported")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .errorCode("METHOD_NOT_ALLOWED")
                .message("HTTP method '" + ex.getMethod() + "' is not supported for this endpoint")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("ENDPOINT_NOT_FOUND")
                .message("No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ========================================
    // ENTITY NOT FOUND (404)
    // ========================================

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            jakarta.persistence.EntityNotFoundException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        
        log.info("[{}] Entity not found: {}", correlationId, ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("RESOURCE_NOT_FOUND")
                .message(ex.getMessage() != null ? ex.getMessage() : "Requested resource not found")
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ========================================
    // DATABASE ERRORS
    // ========================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.DATABASE_CONSTRAINT_VIOLATION;
        
        log.error("[{}] Data integrity violation: {}", correlationId, ex.getMostSpecificCause().getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({PersistenceException.class, DataAccessException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseError(
            Exception ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
        
        log.error("[{}] Database error: {}", correlationId, ex.getMessage(), ex);
        sentryReporter.reportIfNeeded(errorCode, ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.internalServerError().body(response);
    }

    // ========================================
    // CATCH-ALL HANDLER
    // ========================================

    /**
     * Handles all unexpected exceptions.
     * These are logged at ERROR level and reported to Sentry.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
            Exception ex,
            HttpServletRequest request) {
        
        String correlationId = getCorrelationId();
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        
        // Log full stack trace for unexpected errors
        log.error("[{}] Unexpected error on {} {}: {}", 
                correlationId, 
                request.getMethod(), 
                request.getRequestURI(), 
                ex.getMessage(), 
                ex);
        
        // Report to Sentry
        sentryReporter.reportUnexpected(ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(errorCode.name())
                .message(resolveMessage(errorCode))
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.internalServerError().body(response);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Resolves i18n message for the given error code.
     */
    private String resolveMessage(ErrorCode errorCode, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(errorCode.name(), args, locale);
        } catch (Exception e) {
            // Fallback to enum default message
            return errorCode.getDefaultMessage();
        }
    }

    /**
     * Gets correlation ID from MDC (set by CorrelationIdFilter).
     */
    private String getCorrelationId() {
        return Optional.ofNullable(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                .orElse("unknown");
    }

    /**
     * Logs exception with appropriate level based on ErrorCode severity.
     */
    private void logException(ErrorCode errorCode, Exception ex, String correlationId, HttpServletRequest request) {
        String logMessage = String.format("[%s] %s on %s %s: %s",
                correlationId,
                errorCode.name(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        
        switch (errorCode.getSeverity()) {
            case DEBUG -> log.debug(logMessage);
            case INFO -> log.info(logMessage);
            case WARN -> log.warn(logMessage);
            case ERROR, CRITICAL -> log.error(logMessage, ex);
        }
    }

    /**
     * Sanitizes exception context to remove sensitive information before returning to client.
     */
    private Map<String, Object> sanitizeContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        
        // List of sensitive field names to exclude from response
        var sensitiveFields = java.util.Set.of(
            "password", "token", "secret", "key", "authorization",
            "credit_card", "creditCard", "ssn", "pin"
        );
        
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey().toLowerCase();
            boolean isSensitive = sensitiveFields.stream()
                    .anyMatch(key::contains);
            
            if (!isSensitive) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        
        return sanitized.isEmpty() ? null : sanitized;
    }
}
