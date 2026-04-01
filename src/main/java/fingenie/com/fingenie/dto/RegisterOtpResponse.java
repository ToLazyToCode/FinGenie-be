package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOtpResponse {

    private String sessionId;
    private String email;
    private int expiresInSeconds;
    private String message; // e.g. "OTP sent to your email"
}
