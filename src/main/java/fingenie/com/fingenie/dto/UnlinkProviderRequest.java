package fingenie.com.fingenie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for unlinking a provider from account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to unlink an authentication provider from account")
public class UnlinkProviderRequest {

    @NotBlank(message = "Password is required for security verification")
    @Schema(
        description = "User's current password to verify account ownership",
        example = "currentPassword123"
    )
    private String password;
}
