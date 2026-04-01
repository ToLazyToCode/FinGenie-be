package fingenie.com.fingenie.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginRequest {

    @NotBlank(message = "Email/username is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
