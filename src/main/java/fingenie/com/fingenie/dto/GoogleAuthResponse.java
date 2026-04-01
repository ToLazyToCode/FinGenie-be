package fingenie.com.fingenie.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Extended auth response that can indicate account linking requirement
 * 
 * Used when Google login detects an existing account with the same email
 * that requires linking confirmation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication response with potential linking requirement")
public class GoogleAuthResponse {

    // ============ Regular Auth Fields ============
    
    @Schema(description = "JWT access token (null if linking required)")
    private String accessToken;

    @Schema(description = "JWT refresh token (null if linking required)")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token expiration in seconds")
    private Long expiresIn;

    @Schema(description = "User's account ID (null if linking required)")
    private Long accountId;

    @Schema(description = "User's email")
    private String email;

    @Schema(description = "User's full name")
    private String fullName;

    // ============ Linking Required Fields ============

    @Schema(
        description = "Response code: SUCCESS or ACCOUNT_LINK_REQUIRED",
        example = "ACCOUNT_LINK_REQUIRED"
    )
    private String code;

    @Schema(
        description = "Temporary token for linking flow (expires in 10 min)",
        example = "link-token-uuid"
    )
    private String linkToken;

    @Schema(description = "Whether the existing account has a password set")
    private Boolean hasPassword;

    @Schema(description = "Existing providers linked to the account")
    private java.util.List<String> existingProviders;

    // ============ Factory Methods ============

    /**
     * Create successful auth response
     */
    public static GoogleAuthResponse success(
            String accessToken, 
            String refreshToken, 
            long expiresIn,
            Long accountId, 
            String email, 
            String fullName) {
        return GoogleAuthResponse.builder()
                .code("SUCCESS")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .accountId(accountId)
                .email(email)
                .fullName(fullName)
                .build();
    }

    /**
     * Create response indicating account linking is required
     */
    public static GoogleAuthResponse linkRequired(
            String email, 
            String linkToken,
            boolean hasPassword,
            java.util.List<String> existingProviders) {
        return GoogleAuthResponse.builder()
                .code("ACCOUNT_LINK_REQUIRED")
                .email(email)
                .linkToken(linkToken)
                .hasPassword(hasPassword)
                .existingProviders(existingProviders)
                .build();
    }
}
