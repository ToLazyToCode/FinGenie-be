package fingenie.com.fingenie.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for rate limiting
 * 
 * Externalized configuration for flexible rate limit tuning
 * across different environments (dev, staging, prod)
 */
@Configuration
@ConfigurationProperties(prefix = "fingenie.ratelimit")
@Getter
@Setter
public class RateLimitConfig {

    /**
     * Validation endpoints rate limit configuration
     */
    private ValidationLimit validation = new ValidationLimit();

    /**
     * Authentication endpoints rate limit configuration
     */
    private AuthLimit auth = new AuthLimit();

    /**
     * General API rate limit configuration
     */
    private GeneralLimit general = new GeneralLimit();

    /**
     * Rate limit for validation endpoints (email, username checks)
     * Most restrictive to prevent enumeration attacks
     */
    @Getter
    @Setter
    public static class ValidationLimit {
        /**
         * Maximum requests per period (default: 10)
         */
        private int capacity = 10;

        /**
         * Time window in minutes (default: 1)
         */
        private int periodMinutes = 1;

        /**
         * Burst capacity for initial requests (default: 5)
         */
        private int burstCapacity = 5;

        /**
         * Whether to enable rate limiting for validation
         */
        private boolean enabled = true;

        public Duration getPeriod() {
            return Duration.ofMinutes(periodMinutes);
        }
    }

    /**
     * Rate limit for auth endpoints (login, register, password reset)
     */
    @Getter
    @Setter
    public static class AuthLimit {
        /**
         * Maximum login attempts per period
         */
        private int loginCapacity = 5;

        /**
         * Maximum password reset requests per period
         */
        private int passwordResetCapacity = 3;

        /**
         * Time window in minutes
         */
        private int periodMinutes = 15;

        /**
         * Lockout duration after exceeding limits (minutes)
         */
        private int lockoutMinutes = 30;

        private boolean enabled = true;

        public Duration getPeriod() {
            return Duration.ofMinutes(periodMinutes);
        }

        public Duration getLockoutDuration() {
            return Duration.ofMinutes(lockoutMinutes);
        }
    }

    /**
     * General API rate limits
     */
    @Getter
    @Setter
    public static class GeneralLimit {
        /**
         * Requests per minute for general API
         */
        private int requestsPerMinute = 100;

        /**
         * Burst capacity
         */
        private int burstCapacity = 20;

        private boolean enabled = false;
    }
}
