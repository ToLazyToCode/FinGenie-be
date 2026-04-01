package fingenie.com.fingenie.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Validation Response DTOs
 * 
 * Standardized responses for validation endpoints.
 * 
 * SECURITY: Generic error messages to prevent information disclosure:
 * - For email: "Email is not available" (not "Email already exists")
 * - For forgot password: Always success message regardless of email existence
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ValidationResponse(
        /**
         * Whether validation passed
         */
        boolean valid,

        /**
         * Generic validation message (user-facing)
         * Always use generic messages to prevent enumeration
         */
        String message,

        /**
         * Machine-readable code for client handling
         */
        String code,

        /**
         * Suggestions for fixing the issue (optional)
         */
        List<String> suggestions
) {

    /**
     * Create a valid response
     */
    public static ValidationResponse success() {
        return ValidationResponse.builder()
                .valid(true)
                .message("Valid")
                .code("VALID")
                .build();
    }

    /**
     * Create a valid response with message
     */
    public static ValidationResponse success(String message) {
        return ValidationResponse.builder()
                .valid(true)
                .message(message)
                .code("VALID")
                .build();
    }

    /**
     * Create an invalid response with generic message
     */
    public static ValidationResponse invalid(String message) {
        return ValidationResponse.builder()
                .valid(false)
                .message(message)
                .code("VALIDATION_FAILED")
                .build();
    }

    /**
     * Create an invalid response with code
     */
    public static ValidationResponse invalid(String message, String code) {
        return ValidationResponse.builder()
                .valid(false)
                .message(message)
                .code(code)
                .build();
    }

    /**
     * Create an invalid response with suggestions
     */
    public static ValidationResponse invalid(String message, String code, List<String> suggestions) {
        return ValidationResponse.builder()
                .valid(false)
                .message(message)
                .code(code)
                .suggestions(suggestions)
                .build();
    }

    /**
     * Password strength response with detailed analysis
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record PasswordStrength(
            /**
             * Whether password meets minimum requirements
             */
            boolean valid,

            /**
             * Strength score (0-100)
             */
            int score,

            /**
             * Strength level: WEAK, FAIR, GOOD, STRONG
             */
            String level,

            /**
             * Human-readable message
             */
            String message,

            /**
             * Detailed criteria status
             */
            Criteria criteria,

            /**
             * Suggestions to improve password
             */
            List<String> suggestions
    ) {

        /**
         * Password strength criteria breakdown
         */
        @Builder
        public record Criteria(
                boolean hasMinLength,
                boolean hasUppercase,
                boolean hasLowercase,
                boolean hasNumber,
                boolean hasSpecialChar,
                boolean noCommonPatterns
        ) {}

        /**
         * Strength levels
         */
        public enum Level {
            WEAK(0, 29),
            FAIR(30, 59),
            GOOD(60, 79),
            STRONG(80, 100);

            public final int minScore;
            public final int maxScore;

            Level(int minScore, int maxScore) {
                this.minScore = minScore;
                this.maxScore = maxScore;
            }

            public static Level fromScore(int score) {
                for (Level level : values()) {
                    if (score >= level.minScore && score <= level.maxScore) {
                        return level;
                    }
                }
                return WEAK;
            }
        }
    }
}
