package fingenie.com.fingenie.common.error.exceptions;

import fingenie.com.fingenie.common.error.BaseAppException;
import fingenie.com.fingenie.common.error.ErrorCode;

import java.util.Map;

/**
 * Authentication Exceptions
 */
public class AuthenticationExceptions {

    private AuthenticationExceptions() {
        // Utility class
    }

    /**
     * Invalid credentials (wrong email/password)
     */
    public static class InvalidCredentialsException extends BaseAppException {
        public InvalidCredentialsException() {
            super(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    /**
     * Access token expired
     */
    public static class TokenExpiredException extends BaseAppException {
        public TokenExpiredException() {
            super(ErrorCode.AUTH_TOKEN_EXPIRED);
        }
    }

    /**
     * Invalid or malformed token
     */
    public static class InvalidTokenException extends BaseAppException {
        public InvalidTokenException() {
            super(ErrorCode.AUTH_TOKEN_INVALID);
        }

        public InvalidTokenException(Throwable cause) {
            super(ErrorCode.AUTH_TOKEN_INVALID, cause);
        }
    }

    /**
     * Invalid refresh token
     */
    public static class InvalidRefreshTokenException extends BaseAppException {
        public InvalidRefreshTokenException() {
            super(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
    }

    /**
     * Refresh token expired
     */
    public static class RefreshTokenExpiredException extends BaseAppException {
        public RefreshTokenExpiredException() {
            super(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }
    }

    /**
     * Access denied (forbidden)
     */
    public static class AccessDeniedException extends BaseAppException {
        public AccessDeniedException() {
            super(ErrorCode.AUTH_ACCESS_DENIED);
        }

        public AccessDeniedException(String resource) {
            super(ErrorCode.AUTH_ACCESS_DENIED, Map.of("resource", resource));
        }
    }

    /**
     * Session expired
     */
    public static class SessionExpiredException extends BaseAppException {
        public SessionExpiredException() {
            super(ErrorCode.AUTH_SESSION_EXPIRED);
        }
    }

    /**
     * Account locked due to too many failed attempts
     */
    public static class AccountLockedException extends BaseAppException {
        public AccountLockedException() {
            super(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
    }

    /**
     * Account disabled by admin
     */
    public static class AccountDisabledException extends BaseAppException {
        public AccountDisabledException() {
            super(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
    }

    /**
     * Email not verified
     */
    public static class EmailNotVerifiedException extends BaseAppException {
        public EmailNotVerifiedException(String email) {
            super(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, Map.of("email", email));
        }
    }

    /**
     * Invalid OTP
     */
    public static class InvalidOtpException extends BaseAppException {
        public InvalidOtpException() {
            super(ErrorCode.AUTH_OTP_INVALID);
        }
    }

    /**
     * OTP expired
     */
    public static class OtpExpiredException extends BaseAppException {
        public OtpExpiredException() {
            super(ErrorCode.AUTH_OTP_EXPIRED);
        }
    }

    /**
     * Max OTP attempts exceeded
     */
    public static class OtpMaxAttemptsException extends BaseAppException {
        public OtpMaxAttemptsException() {
            super(ErrorCode.AUTH_OTP_MAX_ATTEMPTS);
        }
    }
}
