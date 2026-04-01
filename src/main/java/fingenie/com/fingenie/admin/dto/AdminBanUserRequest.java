package fingenie.com.fingenie.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for banning a user account.
 */
@Data
public class AdminBanUserRequest {

    @NotBlank(message = "Ban reason is required")
    private String reason;
}
