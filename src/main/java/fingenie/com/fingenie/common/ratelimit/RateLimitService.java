package fingenie.com.fingenie.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.local.LocalBucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limit Service
 * 
 * Manages rate limiting buckets per client IP using Bucket4j.
 * Features:
 * - Per-IP rate limiting
 * - Configurable limits per endpoint type
 * - Automatic bucket cleanup (via scheduled task)
 * - Thread-safe operations
 * 
 * SECURITY: Protects against:
 * - Brute force attacks
 * - Account enumeration
 * - API abuse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitConfig config;

    // In-memory bucket storage (per IP)
    // For production with multiple instances, use Redis-backed Bucket4j
    private final Map<String, Bucket> validationBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();

    /**
     * Rate limit types for different endpoint categories
     */
    public enum RateLimitType {
        VALIDATION,
        LOGIN,
        PASSWORD_RESET
    }

    /**
     * Result of rate limit check
     */
    public record RateLimitResult(
            boolean allowed,
            long remainingTokens,
            long retryAfterSeconds
    ) {
        public static RateLimitResult allowed(long remaining) {
            return new RateLimitResult(true, remaining, 0);
        }

        public static RateLimitResult denied(long retryAfter) {
            return new RateLimitResult(false, 0, retryAfter);
        }
    }

    /**
     * Check if request is allowed for validation endpoints
     */
    public RateLimitResult checkValidationLimit(String clientIp) {
        if (!config.getValidation().isEnabled()) {
            return RateLimitResult.allowed(Long.MAX_VALUE);
        }

        Bucket bucket = validationBuckets.computeIfAbsent(
                clientIp,
                this::createValidationBucket
        );

        return consumeToken(bucket);
    }

    /**
     * Check if request is allowed for login endpoint
     */
    public RateLimitResult checkLoginLimit(String clientIp) {
        if (!config.getAuth().isEnabled()) {
            return RateLimitResult.allowed(Long.MAX_VALUE);
        }

        Bucket bucket = loginBuckets.computeIfAbsent(
                clientIp,
                this::createLoginBucket
        );

        return consumeToken(bucket);
    }

    /**
     * Check if request is allowed for password reset
     */
    public RateLimitResult checkPasswordResetLimit(String clientIp) {
        if (!config.getAuth().isEnabled()) {
            return RateLimitResult.allowed(Long.MAX_VALUE);
        }

        Bucket bucket = passwordResetBuckets.computeIfAbsent(
                clientIp,
                this::createPasswordResetBucket
        );

        return consumeToken(bucket);
    }

    /**
     * Generic rate limit check
     */
    public RateLimitResult checkLimit(String clientIp, RateLimitType type) {
        return switch (type) {
            case VALIDATION -> checkValidationLimit(clientIp);
            case LOGIN -> checkLoginLimit(clientIp);
            case PASSWORD_RESET -> checkPasswordResetLimit(clientIp);
        };
    }

    /**
     * Create bucket for validation endpoints
     * Stricter limits: 10 requests/minute with burst of 5
     */
    private Bucket createValidationBucket(String key) {
        RateLimitConfig.ValidationLimit cfg = config.getValidation();

        Bandwidth limit = Bandwidth.builder()
                .capacity(cfg.getCapacity())
                .refillGreedy(cfg.getCapacity(), cfg.getPeriod())
                .build();

        Bandwidth burst = Bandwidth.builder()
                .capacity(cfg.getBurstCapacity())
                .refillGreedy(cfg.getBurstCapacity(), Duration.ofSeconds(10))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .addLimit(burst)
                .build();
    }

    /**
     * Create bucket for login endpoint
     * 5 attempts per 15 minutes
     */
    private Bucket createLoginBucket(String key) {
        RateLimitConfig.AuthLimit cfg = config.getAuth();

        Bandwidth limit = Bandwidth.builder()
                .capacity(cfg.getLoginCapacity())
                .refillGreedy(cfg.getLoginCapacity(), cfg.getPeriod())
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for password reset
     * Most restrictive: 3 requests per 15 minutes
     */
    private Bucket createPasswordResetBucket(String key) {
        RateLimitConfig.AuthLimit cfg = config.getAuth();

        Bandwidth limit = Bandwidth.builder()
                .capacity(cfg.getPasswordResetCapacity())
                .refillGreedy(cfg.getPasswordResetCapacity(), cfg.getPeriod())
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Try to consume a token from the bucket
     */
    private RateLimitResult consumeToken(Bucket bucket) {
        var probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitResult.allowed(probe.getRemainingTokens());
        } else {
            long waitNanos = probe.getNanosToWaitForRefill();
            long waitSeconds = (waitNanos / 1_000_000_000) + 1;
            return RateLimitResult.denied(waitSeconds);
        }
    }

    /**
     * Get remaining tokens for a client (for headers)
     */
    public long getRemainingTokens(String clientIp, RateLimitType type) {
        Map<String, Bucket> buckets = switch (type) {
            case VALIDATION -> validationBuckets;
            case LOGIN -> loginBuckets;
            case PASSWORD_RESET -> passwordResetBuckets;
        };

        Bucket bucket = buckets.get(clientIp);
        if (bucket == null) {
            return switch (type) {
                case VALIDATION -> config.getValidation().getCapacity();
                case LOGIN -> config.getAuth().getLoginCapacity();
                case PASSWORD_RESET -> config.getAuth().getPasswordResetCapacity();
            };
        }

        return bucket.getAvailableTokens();
    }

    /**
     * Clear all buckets (for testing or reset)
     */
    public void clearAllBuckets() {
        validationBuckets.clear();
        loginBuckets.clear();
        passwordResetBuckets.clear();
        log.info("All rate limit buckets cleared");
    }

    /**
     * Clear buckets for a specific IP (for admin use)
     */
    public void clearBucketsForIp(String clientIp) {
        validationBuckets.remove(clientIp);
        loginBuckets.remove(clientIp);
        passwordResetBuckets.remove(clientIp);
        log.info("Rate limit buckets cleared for IP: {}", maskIp(clientIp));
    }

    /**
     * Mask IP for logging (privacy)
     */
    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        return "xxx.xxx.xxx.xxx";
    }
}
