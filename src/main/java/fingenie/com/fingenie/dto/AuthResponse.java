package fingenie.com.fingenie.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    
    // User info
    private Long accountId;
    private String email;
    private String fullName;

    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, Long accountId, String email, String fullName) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.accountId = accountId;
        this.email = email;
        this.fullName = fullName;
    }
}