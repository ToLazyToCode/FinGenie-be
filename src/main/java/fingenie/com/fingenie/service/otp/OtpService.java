package fingenie.com.fingenie.service.otp;

import fingenie.com.fingenie.dto.RegisterRequest;
import fingenie.com.fingenie.common.CustomException;
import fingenie.com.fingenie.constraints.AppMessage;
import fingenie.com.fingenie.service.otp.audit.OtpAuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * OTP Service - Generate, Hash, Validate, Blacklist.
 * Never store raw OTP - only hash (SHA-256).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int OTP_LENGTH = 6;

    private final OtpEmailService otpEmailService;
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);

    private final OtpStore otpStore;
    private final OtpAuditLogger auditLogger;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate 6-digit OTP.
     */
    public String generateOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Hash OTP with SHA-256 - never store raw OTP.
     */
    public String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Create OTP session for Register flow.
     */
    public String createRegisterSession(RegisterRequest request, String passwordHash) {
        String email = request.getEmail();
        checkRateLimit(email, OtpPurpose.REGISTER_EMAIL_VERIFY);

        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        String sessionId = UUID.randomUUID().toString();
        OtpSession session = OtpSession.builder()
                .email(email)
                .otpHash(otpHash)
                .attemptCount(0)
                .expireAt(Instant.now().plus(SESSION_TTL))
                .purpose(OtpPurpose.REGISTER_EMAIL_VERIFY)
                .passwordHash(passwordHash)
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .build();

        otpStore.saveSession(sessionId, session);
        auditLogger.logOtpRequested(email, OtpPurpose.REGISTER_EMAIL_VERIFY, sessionId);

        otpEmailService.sendRegisterOtp(email, otp);
        return sessionId;
    }

    /**
     * Create OTP session for Forgot Password flow.
     */
    public String createForgotPasswordSession(String email) {
        checkRateLimit(email, OtpPurpose.FORGOT_PASSWORD);

        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        String sessionId = UUID.randomUUID().toString();
        OtpSession session = OtpSession.builder()
                .email(email)
                .otpHash(otpHash)
                .attemptCount(0)
                .expireAt(Instant.now().plus(SESSION_TTL))
                .purpose(OtpPurpose.FORGOT_PASSWORD)
                .build();

        otpStore.saveSession(sessionId, session);
        auditLogger.logOtpRequested(email, OtpPurpose.FORGOT_PASSWORD, sessionId);

        otpEmailService.sendForgotPasswordOtp(email, otp);
        return sessionId;
    }

    /**
     * Validate OTP - check blacklist BEFORE success verify.
     */
    public OtpSession validateOtp(String sessionId, String otpInput, OtpPurpose expectedPurpose) {
        OtpSession session = otpStore.getSession(sessionId)
                .orElseThrow(() -> {
                    auditLogger.logOtpFailedAttempt(sessionId, "SESSION_NOT_FOUND");
                    throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid or expired OTP");
                });

        if (session.getExpireAt().isBefore(Instant.now())) {
            otpStore.deleteSession(sessionId);
            auditLogger.logOtpFailedAttempt(sessionId, "EXPIRED");
            throw new CustomException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "OTP has expired. Please request a new one.");
        }

        if (session.getAttemptCount() >= MAX_ATTEMPTS) {
            otpStore.deleteSession(sessionId);
            auditLogger.logOtpFailedAttempt(sessionId, "MAX_ATTEMPTS_EXCEEDED");
            throw new CustomException(HttpStatus.BAD_REQUEST, "OTP_MAX_ATTEMPTS", "Too many attempts. Please request a new OTP.");
        }

        if (session.getPurpose() != expectedPurpose) {
            auditLogger.logOtpFailedAttempt(sessionId, "PURPOSE_MISMATCH");
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid OTP");
        }

        String otpHashInput = hashOtp(otpInput);

        // CRITICAL: Check blacklist BEFORE success verify
        if (otpStore.isBlacklisted(otpHashInput)) {
            otpStore.deleteSession(sessionId);
            auditLogger.logOtpBlacklistedHit(session.getEmail(), sessionId);
            throw new CustomException(HttpStatus.BAD_REQUEST, "OTP_ALREADY_USED", "This OTP has already been used.");
        }

        if (!session.getOtpHash().equals(otpHashInput)) {
            session.setAttemptCount(session.getAttemptCount() + 1);
            otpStore.saveSession(sessionId, session);
            auditLogger.logOtpFailedAttempt(sessionId, "INVALID_CODE");
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid OTP code");
        }

        // Success - add to blacklist BEFORE returning (prevent reuse)
        otpStore.addToBlacklist(session.getOtpHash(), Duration.between(Instant.now(), session.getExpireAt()));
        otpStore.deleteSession(sessionId);
        auditLogger.logOtpVerified(session.getEmail(), session.getPurpose(), sessionId);

        return session;
    }

    /**
     * Resend OTP - enforces 60s cooldown.
     */
    public String resendOtp(String sessionId, OtpPurpose purpose) {
        OtpSession existing = otpStore.getSession(sessionId)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "SESSION_EXPIRED", "Session expired. Please start again."));

        if (!otpStore.canResend(existing.getEmail())) {
            auditLogger.logOtpRateLimited(existing.getEmail(), "RESEND_COOLDOWN");
            throw new CustomException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_COOLDOWN", "Please wait 60 seconds before requesting a new OTP.");
        }

        otpStore.setResendCooldown(existing.getEmail());

        // Create new session with same purpose and data
        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        String newSessionId = UUID.randomUUID().toString();
        OtpSession newSession = OtpSession.builder()
                .email(existing.getEmail())
                .otpHash(otpHash)
                .attemptCount(0)
                .expireAt(Instant.now().plus(SESSION_TTL))
                .purpose(purpose)
                .passwordHash(existing.getPasswordHash())
                .fullName(existing.getFullName())
                .dateOfBirth(existing.getDateOfBirth())
                .build();

        otpStore.deleteSession(sessionId);
        otpStore.saveSession(newSessionId, newSession);
        auditLogger.logOtpRequested(existing.getEmail(), purpose, newSessionId);

        if (purpose == OtpPurpose.REGISTER_EMAIL_VERIFY) {
            otpEmailService.sendRegisterOtp(existing.getEmail(), otp);
        } else if (purpose == OtpPurpose.FORGOT_PASSWORD) {
            otpEmailService.sendForgotPasswordOtp(existing.getEmail(), otp);
        }

        return newSessionId;
    }

    public OtpSession getSessionForResend(String sessionId) {
        return otpStore.getSession(sessionId)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "SESSION_EXPIRED", "Session expired."));
    }

    private void checkRateLimit(String email, OtpPurpose purpose) {
        if (!otpStore.checkAndIncrementRate(email)) {
            auditLogger.logOtpRateLimited(email, "RATE_LIMIT");
            throw new CustomException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED", "Too many OTP requests. Please try again later.");
        }
    }
}
