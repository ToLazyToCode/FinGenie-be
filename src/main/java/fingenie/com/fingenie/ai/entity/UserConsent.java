package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * UserConsent - GDPR-compliant consent tracking for AI features.
 * 
 * Consent types:
 * - AI_TRAINING: Allow data to be used for model training
 * - AI_PREDICTIONS: Allow AI to make spending predictions
 * - ANALYTICS: Allow usage analytics
 * - PERSONALIZATION: Allow personalized recommendations
 * - MARKETING: Allow marketing communications
 * 
 * Supports:
 * - Consent granting and withdrawal
 * - Audit trail with timestamps
 * - IP address tracking for compliance
 */
@Entity
@Table(name = "user_consent", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "consent_type"}),
    indexes = {
        @Index(name = "idx_user_consent_user", columnList = "user_id"),
        @Index(name = "idx_user_consent_type", columnList = "consent_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "consent_type", length = 50, nullable = false)
    private String consentType; // AI_TRAINING, AI_PREDICTIONS, ANALYTICS, PERSONALIZATION, MARKETING

    @Column(name = "granted", nullable = false)
    @Builder.Default
    private Boolean granted = false;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "grant_ip_address", length = 45)
    private String grantIpAddress;

    @Column(name = "withdraw_ip_address", length = 45)
    private String withdrawIpAddress;

    @Column(name = "consent_version", length = 20)
    @Builder.Default
    private String consentVersion = "v1"; // Version of consent policy

    @Column(name = "consent_text_hash", length = 64)
    private String consentTextHash; // Hash of the consent text shown to user

    @Column(name = "grant_source", length = 50)
    private String grantSource; // REGISTRATION, SETTINGS, POPUP, API

    @Column(name = "withdraw_reason", length = 200)
    private String withdrawReason;

    // === Helper Methods ===
    
    public boolean isCurrentlyGranted() {
        return Boolean.TRUE.equals(granted) && withdrawnAt == null;
    }

    public void grant(String ipAddress, String source) {
        this.granted = true;
        this.grantedAt = LocalDateTime.now();
        this.grantIpAddress = ipAddress;
        this.grantSource = source;
        this.withdrawnAt = null;
        this.withdrawIpAddress = null;
        this.withdrawReason = null;
    }

    public void withdraw(String ipAddress, String reason) {
        this.granted = false;
        this.withdrawnAt = LocalDateTime.now();
        this.withdrawIpAddress = ipAddress;
        this.withdrawReason = reason;
    }
}
