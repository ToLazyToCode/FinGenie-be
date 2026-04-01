package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordVerifyResponse {

    private String resetToken;  // short-lived token to allow password reset
    private String message;    // e.g. "OTP verified. You can now reset your password"
}
