package fingenie.com.fingenie.service.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based OTP storage.
 * - otp:session:{sessionId} - TTL 5 min
 * - otp:rate:{email} - TTL 10 min, max 5 requests
 * - otp:blacklist:{otpHash} - TTL same as original OTP
 * - otp:resend:{email} - TTL 60s (cooldown)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OtpStore {

    private static final String SESSION_PREFIX = "otp:session:";
    private static final String RATE_PREFIX = "otp:rate:";
    private static final String BLACKLIST_PREFIX = "otp:blacklist:";
    private static final String RESEND_PREFIX = "otp:resend:";

    private static final Duration SESSION_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_TTL = Duration.ofMinutes(10);
    private static final int RATE_MAX_ATTEMPTS = 5;
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveSession(String sessionId, OtpSession session) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, session, SESSION_TTL);
        log.debug("OTP session saved: {}", sessionId);
    }

    public Optional<OtpSession> getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? Optional.of((OtpSession) val) : Optional.empty();
    }

    public void deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.debug("OTP session deleted: {}", sessionId);
    }

    /**
     * Rate limit: max 5 requests per email per 10 minutes.
     * Returns true if allowed, false if rate limited.
     */
    public boolean checkAndIncrementRate(String email) {
        String key = RATE_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) count = 1L;
        if (count == 1) {
            redisTemplate.expire(key, RATE_TTL);
        }
        return count <= RATE_MAX_ATTEMPTS;
    }

    public int getRateCount(String email) {
        String key = RATE_PREFIX + email;
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? ((Number) val).intValue() : 0;
    }

    /**
     * Blacklist OTP hash after successful use - prevent reuse.
     */
    public void addToBlacklist(String otpHash, Duration ttl) {
        String key = BLACKLIST_PREFIX + otpHash;
        redisTemplate.opsForValue().set(key, "1", ttl);
        log.debug("OTP blacklisted (hash): {}", otpHash.substring(0, Math.min(8, otpHash.length())) + "...");
    }

    public boolean isBlacklisted(String otpHash) {
        String key = BLACKLIST_PREFIX + otpHash;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Resend cooldown: 60 seconds.
     */
    public boolean canResend(String email) {
        String key = RESEND_PREFIX + email;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setResendCooldown(String email) {
        String key = RESEND_PREFIX + email;
        redisTemplate.opsForValue().set(key, "1", RESEND_COOLDOWN);
    }
}
