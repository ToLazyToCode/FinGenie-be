package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.constraints.AppConstraint;
import fingenie.com.fingenie.constraints.AppMessage;
import fingenie.com.fingenie.constraints.AppPattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {

    @NotBlank(message = AppMessage.REQUIRED_EMAIL)
    @Email(message = AppMessage.INVALID_EMAIL)
    private String email;

    @NotBlank(message = AppMessage.REQUIRED_PASSWORD)
    @Size(min = AppConstraint.PASSWORD_MIN_LENGTH, message = AppMessage.INVALID_PASSWORD_LENGTH)
    private String password;

    @NotBlank(message = AppMessage.REQUIRED_CONFIRM_PASSWORD)
    private String confirmPassword;

    @NotBlank(message = AppMessage.REQUIRED_NAME)
    private String fullName;

    @NotBlank(message = AppMessage.INVALID_DATE)
    @Pattern(regexp = AppPattern.DATE_PATTERN, message = AppMessage.INVALID_DATE)
    private String dateOfBirth;

}