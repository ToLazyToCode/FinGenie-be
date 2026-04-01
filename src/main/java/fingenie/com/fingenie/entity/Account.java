package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "account")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password")
    private String password;  // Nullable for OAuth users

    @Column(name = "name")
    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "role")
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "last_login")
    private Timestamp lastLogin;

    @Column(name = "login_attempt")
    @Builder.Default
    private int loginAttempt = 0;

    // ============ OAuth2 Fields ============
    
    /**
     * Authentication provider: LOCAL (email/password), GOOGLE, etc.
     */
    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    /**
     * External provider's user ID (e.g., Google sub claim)
     * Only populated for OAuth accounts
     */
    @Column(name = "provider_id")
    private String providerId;

    /**
     * Whether email has been verified
     * OAuth emails are auto-verified by provider
     */
    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Whether account is Premium (for paid features)
      */
    @Column(name = "is_premium")
    @Builder.Default
    private boolean isPremium = false;

    public enum AuthProvider {
        LOCAL,    // Email/password registration
        GOOGLE    // Google OAuth2
    }

    public enum Role {
        USER("user"), MODERATOR("mod"), ADMIN("admin");

        Role(String role) {
        }

        public String getRole() {
            return this.name();
        }

        public static Role getRole(String role) {
            return Role.valueOf(role);
        }

        public boolean isAdmin() {
            return this == ADMIN;
        }

        public boolean isUser() {
            return this == USER;
        }
    }

    /**
     * Check if this is an OAuth account (no local password)
     */
    public boolean isOAuthAccount() {
        return provider != AuthProvider.LOCAL;
    }
}
