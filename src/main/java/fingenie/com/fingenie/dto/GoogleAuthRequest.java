package fingenie.com.fingenie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google OAuth2 authentication.
 * 
 * Client obtains ID token from Google Sign-In SDK and sends it here.
 * Backend verifies the token with Google and extracts user info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Google OAuth2 authentication request")
public class GoogleAuthRequest {

    @NotBlank(message = "ID token is required")
    @Schema(
        description = "Google ID token obtained from Google Sign-In SDK",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String idToken;

    @Schema(
        description = "Optional: Client platform for analytics (ios, android, web)",
        example = "android"
    )
    private String platform;

    @Schema(
        description = "Device identifier for token binding and multi-device management",
        example = "device_abc123xyz"
    )
    private String deviceId;
}
