package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.entity.Account;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Refresh Token Entity with Rotation Support
 * 
 * SECURITY FEATURES:
 * - Token stored as SHA-256 hash (not plain text)
 * - Device binding for multi-device support
 * - Rotation tracking for theft detection
 * - Family tree for cascade revocation
 * 
 * TOKEN THEFT DETECTION:
 * If a refresh token that has been rotated (replaced) is reused,
 * it indicates token theft. All tokens in the family are revoked.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_account", columnList = "account_id"),
        @Index(name = "idx_refresh_tokens_device", columnList = "device_id"),
        @Index(name = "idx_refresh_tokens_family", columnList = "token_family")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SHA-256 hash of the refresh token
     * NEVER store plain text tokens
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * Device identifier for multi-device support
     * Allows users to have separate sessions per device
     */
    @Column(name = "device_id", length = 255)
    private String deviceId;

    /**
     * Device information (user agent, platform)
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    /**
     * IP address at token creation
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Reason for revocation (for audit)
     */
    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;

    /**
     * When the token was revoked
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Token family ID for rotation tracking
     * All rotated tokens share the same family ID
     * Used for cascade revocation on theft detection
     */
    @Column(name = "token_family", nullable = false, length = 36)
    private String tokenFamily;

    /**
     * ID of the token that replaced this one (rotation)
     * Null if this is the current active token
     */
    @Column(name = "replaced_by_id")
    private Long replacedById;

    /**
     * Generation number in the rotation chain
     * Increments with each rotation
     */
    @Column(name = "generation", nullable = false)
    @Builder.Default
    private int generation = 0;

    /**
     * Whether this token has been used for rotation
     * If true and used again = THEFT DETECTED
     */
    @Column(name = "rotated", nullable = false)
    @Builder.Default
    private boolean rotated = false;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Check if token is valid (not expired, not revoked)
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /**
     * Revoke this token with a reason
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedReason = reason;
        this.revokedAt = Instant.now();
    }

    /**
     * Mark as rotated (replaced by new token)
     */
    public void markRotated(Long newTokenId) {
        this.rotated = true;
        this.replacedById = newTokenId;
    }
}


