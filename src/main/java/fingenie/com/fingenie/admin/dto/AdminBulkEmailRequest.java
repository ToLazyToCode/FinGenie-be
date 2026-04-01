package fingenie.com.fingenie.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request body for sending a bulk email to multiple users.
 */
@Data
public class AdminBulkEmailRequest {

    @NotEmpty(message = "User ID list must not be empty")
    private List<Long> userIds;

    @NotBlank(message = "Email subject is required")
    private String subject;

    @NotBlank(message = "Email content is required")
    private String content;
}
