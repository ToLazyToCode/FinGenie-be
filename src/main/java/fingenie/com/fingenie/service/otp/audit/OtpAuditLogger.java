package fingenie.com.fingenie.service.otp.audit;

import fingenie.com.fingenie.service.otp.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OTP audit logging - never log raw OTP.
 */
@Component
@Slf4j
public class OtpAuditLogger {

    public void logOtpRequested(String email, OtpPurpose purpose, String sessionId) {
        log.info("OTP_AUDIT: otp_requested | email={} | purpose={} | sessionId={}", maskEmail(email), purpose, sessionId);
    }

    public void logOtpVerified(String email, OtpPurpose purpose, String sessionId) {
        log.info("OTP_AUDIT: otp_verified | email={} | purpose={} | sessionId={}", maskEmail(email), purpose, sessionId);
    }

    public void logOtpFailedAttempt(String sessionId, String reason) {
        log.warn("OTP_AUDIT: otp_failed_attempt | sessionId={} | reason={}", sessionId, reason);
    }

    public void logOtpBlacklistedHit(String email, String sessionId) {
        log.warn("OTP_AUDIT: otp_blacklisted_hit | email={} | sessionId={}", maskEmail(email), sessionId);
    }

    public void logOtpRateLimited(String email, String reason) {
        log.warn("OTP_AUDIT: otp_rate_limited | email={} | reason={}", maskEmail(email), reason);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf("@");
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "***" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
