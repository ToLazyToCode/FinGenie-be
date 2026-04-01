package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User information extracted from verified Google ID token.
 * 
 * Contains standard OIDC claims from Google.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {

    /**
     * Google's unique user identifier (sub claim)
     * Used to match returning users
     */
    private String id;

    /**
     * User's email address
     * Google guarantees this is verified
     */
    private String email;

    /**
     * Whether Google has verified this email
     * Should always be true for Google accounts
     */
    private boolean emailVerified;

    /**
     * User's display name
     */
    private String name;

    /**
     * Given (first) name
     */
    private String givenName;

    /**
     * Family (last) name
     */
    private String familyName;

    /**
     * URL to user's Google profile picture
     */
    private String pictureUrl;

    /**
     * User's locale (e.g., "en", "vi")
     */
    private String locale;
}
