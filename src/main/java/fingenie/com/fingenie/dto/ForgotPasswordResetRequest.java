package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.constraints.AppConstraint;
import fingenie.com.fingenie.constraints.AppMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordResetRequest {

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = AppMessage.REQUIRED_PASSWORD)
    @Size(min = AppConstraint.PASSWORD_MIN_LENGTH, message = AppMessage.INVALID_PASSWORD_LENGTH)
    private String newPassword;

    @NotBlank(message = AppMessage.REQUIRED_CONFIRM_PASSWORD)
    private String confirmPassword;
}
