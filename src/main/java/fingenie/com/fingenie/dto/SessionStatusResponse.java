package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for session validity check endpoint.
 * Used for heartbeat and cross-device logout detection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusResponse {
    
    /**
     * Whether the current session is valid
     */
    private boolean valid;
    
    /**
     * Reason for invalid session (null if valid)
     */
    private SessionInvalidReason reason;
    
    /**
     * Reasons why a session may be invalid
     */
    public enum SessionInvalidReason {
        ACCOUNT_DISABLED,
        SESSION_EXPIRED,
        CONCURRENT_SESSION,
        TOKEN_REVOKED
    }
    
    /**
     * Create a valid session response
     */
    public static SessionStatusResponse valid() {
        return SessionStatusResponse.builder()
                .valid(true)
                .build();
    }
    
    /**
     * Create an invalid session response with reason
     */
    public static SessionStatusResponse invalid(SessionInvalidReason reason) {
        return SessionStatusResponse.builder()
                .valid(false)
                .reason(reason)
                .build();
    }
}
