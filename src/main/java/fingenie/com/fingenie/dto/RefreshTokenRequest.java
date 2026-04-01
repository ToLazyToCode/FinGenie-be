package fingenie.com.fingenie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for token refresh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to refresh access token using refresh token")
public class RefreshTokenRequest {

    @Schema(
        description = "The refresh token to use for generating new tokens",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String refreshToken;

    @Schema(
        description = "Device ID for session binding",
        example = "device-uuid-1234"
    )
    private String deviceId;
}
