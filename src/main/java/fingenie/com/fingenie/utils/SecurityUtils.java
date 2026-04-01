package fingenie.com.fingenie.utils;

import fingenie.com.fingenie.entity.AuthAccount;
import fingenie.com.fingenie.entity.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security-related operations.
 */
@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Get the current authenticated Account entity.
     * Requires AuthAccount as the principal.
     */
    public static Account getCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthAccount auth)) {
            throw new RuntimeException("Not authenticated");
        }
        
        return auth.getAccount();
    }

    /**
     * Get the current authenticated account ID.
     * Uses AuthAccount principal to get the Account entity and extract ID.
     */
    public static Long getCurrentAccountId() {
        log.info( "Current account ID: {}", getCurrentAccount().getId());
        return getCurrentAccount().getId();
    }

    /**
     * Get the current authenticated email if available.
     */
    public static String getCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }

    /**
     * Check if the current user is authenticated.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
