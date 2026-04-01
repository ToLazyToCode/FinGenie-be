package fingenie.com.fingenie.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request body for bulk-banning multiple users.
 */
@Data
public class AdminBulkBanRequest {

    @NotEmpty(message = "User ID list must not be empty")
    private List<Long> userIds;

    @NotBlank(message = "Ban reason is required")
    private String reason;
}
