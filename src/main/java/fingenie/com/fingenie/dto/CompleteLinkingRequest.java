package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for completing Google account linking.
 * 
 * This is used after receiving ACCOUNT_LINK_REQUIRED response from Google login
 * when the email already exists with a password-protected account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteLinkingRequest {

    /**
     * The link token received from the ACCOUNT_LINK_REQUIRED response.
     * This token is single-use and expires in 10 minutes.
     */
    @NotBlank(message = "Link token is required")
    private String linkToken;

    /**
     * The password for the existing account.
     * Required to verify ownership before linking.
     */
    @NotBlank(message = "Password is required to verify account ownership")
    private String password;

    /**
     * Device identifier for token binding (optional but recommended).
     * Used for multi-device session management.
     */
    private String deviceId;
}
