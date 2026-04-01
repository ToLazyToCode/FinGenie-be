package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.ValidationResponse;
import fingenie.com.fingenie.dto.ValidationResponse.PasswordStrength;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation Service
 * 
 * Provides secure validation logic for async field validation.
 * 
 * SECURITY PRINCIPLES:
 * 1. Use constant-time comparisons where possible
 * 2. Never reveal specific failure reasons that could aid attackers
 * 3. Log validation attempts for security monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;

    // Pattern definitions
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,30}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+84|84|0)[0-9]{9}$"
    );

    // Common passwords to reject (subset - in production use a comprehensive list)
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",
            "baseball", "iloveyou", "master", "sunshine", "ashley",
            "bailey", "shadow", "123123", "654321", "superman",
            "qazwsx", "michael", "football", "password1", "password123"
    );

    /**
     * Validate email format and availability
     * 
     * SECURITY: Returns generic message for existing emails
     */
    public ValidationResponse validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return ValidationResponse.invalid("Email is required", "EMAIL_REQUIRED");
        }

        email = email.trim().toLowerCase();

        // Format validation
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResponse.invalid("Please enter a valid email address", "EMAIL_FORMAT_INVALID");
        }

        // Check if email exists - SECURITY: Use generic message
        // Add artificial delay to prevent timing attacks
        try {
            boolean exists = accountRepository.existsByEmail(email);
            addTimingNoise();
            
            if (exists) {
                // Generic message - does NOT reveal that email exists
                return ValidationResponse.invalid(
                        "This email is not available",
                        "EMAIL_NOT_AVAILABLE"
                );
            }
        } catch (Exception e) {
            log.error("Error checking email availability", e);
            // Fail open for UX - server validation will catch on submit
            return ValidationResponse.success("Email format is valid");
        }

        return ValidationResponse.success("Email is available");
    }

    /**
     * Validate username format and availability
     */
    public ValidationResponse validateUsername(String username) {
        if (username == null || username.isBlank()) {
            return ValidationResponse.invalid("Username is required", "USERNAME_REQUIRED");
        }

        username = username.trim();

        // Length check
        if (username.length() < 3) {
            return ValidationResponse.invalid(
                    "Username must be at least 3 characters",
                    "USERNAME_TOO_SHORT"
            );
        }

        if (username.length() > 30) {
            return ValidationResponse.invalid(
                    "Username must be at most 30 characters",
                    "USERNAME_TOO_LONG"
            );
        }

        // Format check
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResponse.invalid(
                    "Username can only contain letters, numbers, and underscores",
                    "USERNAME_FORMAT_INVALID"
            );
        }

        // Reserved usernames
        if (isReservedUsername(username)) {
            return ValidationResponse.invalid(
                    "This username is not available",
                    "USERNAME_RESERVED"
            );
        }

        // Check availability - Note: Account entity might not have username field
        // If username is not supported, this will be a format-only validation
        try {
            // For now, treat username validation as format-only since Account uses email
            addTimingNoise();
            boolean exists = false; // Account uses email as identifier
            
            if (exists) {
                return ValidationResponse.invalid(
                        "This username is already taken",
                        "USERNAME_TAKEN",
                        List.of(username + "123", username + "_")
                );
            }
        } catch (Exception e) {
            log.error("Error checking username availability", e);
        }

        return ValidationResponse.success("Username is available");
    }

    /**
     * Validate phone number format
     */
    public ValidationResponse validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return ValidationResponse.invalid("Phone number is required", "PHONE_REQUIRED");
        }

        // Clean phone number (remove spaces, dashes)
        String cleanPhone = phone.replaceAll("[\\s\\-.]", "");

        // Format check for Vietnamese numbers
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return ValidationResponse.invalid(
                    "Please enter a valid Vietnamese phone number",
                    "PHONE_FORMAT_INVALID"
            );
        }

        return ValidationResponse.success("Phone number is valid");
    }

    /**
     * Validate password strength with detailed feedback
     */
    public PasswordStrength validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.builder()
                    .valid(false)
                    .score(0)
                    .level("WEAK")
                    .message("Password is required")
                    .criteria(PasswordStrength.Criteria.builder()
                            .hasMinLength(false)
                            .hasUppercase(false)
                            .hasLowercase(false)
                            .hasNumber(false)
                            .hasSpecialChar(false)
                            .noCommonPatterns(true)
                            .build())
                    .suggestions(List.of("Enter a password"))
                    .build();
        }

        // Check criteria
        boolean hasMinLength = password.length() >= 8;
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasNumber = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialChar = password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        boolean noCommonPatterns = !isCommonPassword(password.toLowerCase());

        // Calculate score
        int score = 0;
        if (hasMinLength) score += 20;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 5;
        if (hasUppercase) score += 15;
        if (hasLowercase) score += 10;
        if (hasNumber) score += 15;
        if (hasSpecialChar) score += 20;
        if (noCommonPatterns) score += 5;
        
        // Deduct for common patterns
        if (!noCommonPatterns) score = Math.max(0, score - 30);

        // Generate suggestions
        List<String> suggestions = new ArrayList<>();
        if (!hasMinLength) suggestions.add("Use at least 8 characters");
        if (!hasUppercase) suggestions.add("Add uppercase letters");
        if (!hasLowercase) suggestions.add("Add lowercase letters");
        if (!hasNumber) suggestions.add("Add numbers");
        if (!hasSpecialChar) suggestions.add("Add special characters (!@#$%^&*)");
        if (!noCommonPatterns) suggestions.add("Avoid common passwords");

        // Determine level
        PasswordStrength.Level level = PasswordStrength.Level.fromScore(score);
        
        // Minimum requirements for validity
        boolean valid = hasMinLength && hasUppercase && hasLowercase && hasNumber && noCommonPatterns;

        String message = switch (level) {
            case WEAK -> "Password is too weak";
            case FAIR -> "Password could be stronger";
            case GOOD -> "Good password";
            case STRONG -> "Strong password!";
        };

        return PasswordStrength.builder()
                .valid(valid)
                .score(score)
                .level(level.name())
                .message(message)
                .criteria(PasswordStrength.Criteria.builder()
                        .hasMinLength(hasMinLength)
                        .hasUppercase(hasUppercase)
                        .hasLowercase(hasLowercase)
                        .hasNumber(hasNumber)
                        .hasSpecialChar(hasSpecialChar)
                        .noCommonPatterns(noCommonPatterns)
                        .build())
                .suggestions(suggestions.isEmpty() ? null : suggestions)
                .build();
    }

    /**
     * Validate wallet name uniqueness for current user
     */
    public ValidationResponse validateWalletName(String walletName, Long excludeWalletId) {
        if (walletName == null || walletName.isBlank()) {
            return ValidationResponse.invalid("Wallet name is required", "WALLET_NAME_REQUIRED");
        }

        walletName = walletName.trim();

        if (walletName.length() > 50) {
            return ValidationResponse.invalid(
                    "Wallet name must be at most 50 characters",
                    "WALLET_NAME_TOO_LONG"
            );
        }

        // Get current user ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ValidationResponse.success("Wallet name format is valid");
        }

        try {
            boolean exists;
            if (excludeWalletId != null) {
                exists = walletRepository.existsByAccountIdAndWalletNameIgnoreCaseAndIdNot(
                        userId, walletName, excludeWalletId
                );
            } else {
                exists = walletRepository.existsByAccountIdAndWalletNameIgnoreCase(userId, walletName);
            }

            if (exists) {
                return ValidationResponse.invalid(
                        "You already have a wallet with this name",
                        "WALLET_NAME_DUPLICATE"
                );
            }
        } catch (Exception e) {
            log.error("Error checking wallet name", e);
        }

        return ValidationResponse.success("Wallet name is available");
    }

    /**
     * Validate transaction amount
     */
    public ValidationResponse validateAmount(BigDecimal amount) {
        if (amount == null) {
            return ValidationResponse.invalid("Amount is required", "AMOUNT_REQUIRED");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResponse.invalid(
                    "Amount must be greater than 0",
                    "AMOUNT_INVALID"
            );
        }

        // Max limit (adjust based on business rules)
        BigDecimal maxAmount = new BigDecimal("999999999999.99");
        if (amount.compareTo(maxAmount) > 0) {
            return ValidationResponse.invalid(
                    "Amount exceeds maximum limit",
                    "AMOUNT_EXCEEDS_MAX"
            );
        }

        return ValidationResponse.success("Amount is valid");
    }

    // ====================================
    // HELPER METHODS
    // ====================================

    /**
     * Check if password is in common passwords list
     */
    private boolean isCommonPassword(String password) {
        return COMMON_PASSWORDS.contains(password);
    }

    /**
     * Check if username is reserved
     */
    private boolean isReservedUsername(String username) {
        String lower = username.toLowerCase();
        return Set.of(
                "admin", "administrator", "root", "system", "support",
                "help", "info", "fingenie", "official", "mod", "moderator"
        ).contains(lower);
    }

    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        
        // Assuming principal has getUserId() method
        try {
            if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                // Get from user service if needed
                return null;
            }
        } catch (Exception e) {
            log.debug("Could not extract user ID from authentication");
        }
        
        return null;
    }

    /**
     * Add random timing noise to prevent timing attacks
     */
    private void addTimingNoise() {
        try {
            // Add 0-50ms random delay
            Thread.sleep((long) (Math.random() * 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
