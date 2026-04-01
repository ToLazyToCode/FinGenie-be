package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Validation Request DTOs
 * 
 * Separate records for each validation type to support
 * type-safe request handling and OpenAPI documentation.
 */
public sealed interface ValidationRequest {

    /**
     * Email validation request
     */
    record Email(
            @NotBlank(message = "Email is required")
            @jakarta.validation.constraints.Email(message = "Invalid email format")
            String email
    ) implements ValidationRequest {}

    /**
     * Username validation request
     */
    record Username(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
            String username
    ) implements ValidationRequest {}

    /**
     * Phone validation request
     */
    record Phone(
            @NotBlank(message = "Phone number is required")
            String phone
    ) implements ValidationRequest {}

    /**
     * Password validation request
     */
    record Password(
            @NotBlank(message = "Password is required")
            String password
    ) implements ValidationRequest {}

    /**
     * Wallet name validation request
     */
    record WalletName(
            @NotBlank(message = "Wallet name is required")
            @Size(min = 1, max = 50, message = "Wallet name must be 1-50 characters")
            String walletName,

            Long excludeWalletId // Optional: exclude this wallet from uniqueness check (for updates)
    ) implements ValidationRequest {}

    /**
     * Amount validation request
     */
    record Amount(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            @DecimalMax(value = "999999999999.99", message = "Amount exceeds maximum limit")
            BigDecimal amount
    ) implements ValidationRequest {}
}
