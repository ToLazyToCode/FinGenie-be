package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.constraints.AppMessage;
import fingenie.com.fingenie.constraints.AppPattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = AppMessage.REQUIRED_EMAIL)
    @Email(message = AppMessage.INVALID_EMAIL)
    private String email;

    @Pattern(regexp = AppPattern.PASSWORD_PATTERN, message = AppMessage.INVALID_PASSWORD_LENGTH)
    @NotBlank(message = AppMessage.REQUIRED_PASSWORD)
    private String password;

}
