package fingenie.com.fingenie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for linking Google account to existing email/password account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to link Google account to existing account")
public class LinkGoogleAccountRequest {

    @NotBlank(message = "Google ID token is required")
    @Schema(
        description = "The ID token received from Google Sign-In",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String idToken;

    @NotBlank(message = "Current password is required for security verification")
    @Schema(
        description = "User's current password to verify account ownership",
        example = "currentPassword123"
    )
    private String password;

    @Schema(
        description = "Device ID for session binding",
        example = "device-uuid-1234"
    )
    private String deviceId;
}
