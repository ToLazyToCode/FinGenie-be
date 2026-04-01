package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * User Provider Entity - Multi-provider authentication support
 * 
 * Allows users to link multiple authentication providers to a single account:
 * - Email/password (LOCAL)
 * - Google OAuth
 * - Future: Apple, Facebook, etc.
 * 
 * SECURITY NOTES:
 * - provider + providerUserId must be unique
 * - email_verified from provider is stored
 * - Prevents account takeover through unverified emails
 */
@Entity
@Table(
    name = "user_providers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_provider_user_id",
            columnNames = {"provider", "provider_user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_user_providers_account_id", columnList = "account_id"),
        @Index(name = "idx_user_providers_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProvider extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * Authentication provider type
     */
    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProviderType provider;

    /**
     * Unique user ID from the provider (e.g., Google 'sub' claim)
     */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    /**
     * Email associated with this provider account
     */
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * Whether the email was verified by the provider
     * CRITICAL: Only trust verified emails for account linking
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Display name from provider (optional)
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Profile picture URL from provider (optional)
     */
    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    /**
     * Provider access token (encrypted, for provider API calls)
     * Only stored if needed for ongoing API access
     */
    @Column(name = "access_token", length = 2000)
    private String accessToken;

    /**
     * Provider refresh token (encrypted)
     */
    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    /**
     * Whether this provider link is active
     * Can be disabled without deletion for audit purposes
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum ProviderType {
        LOCAL,    // Email/password
        GOOGLE,   // Google OAuth2
        APPLE,    // Apple Sign-In (future)
        FACEBOOK  // Facebook Login (future)
    }

    /**
     * Check if this is an OAuth provider (not local email/password)
     */
    public boolean isOAuthProvider() {
        return provider != ProviderType.LOCAL;
    }
}
