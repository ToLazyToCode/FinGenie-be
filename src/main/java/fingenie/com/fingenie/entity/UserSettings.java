package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * User settings entity for storing user preferences.
 */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    // Display Preferences
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en-US";

    @Column(name = "theme", length = 20)
    @Builder.Default
    private String theme = "auto";

    @Column(name = "date_format", length = 20)
    @Builder.Default
    private String dateFormat = "MM/dd/yyyy";

    // Privacy Settings
    @Column(name = "show_profile_publicly")
    @Builder.Default
    private Boolean showProfilePublicly = true;

    @Column(name = "show_in_leaderboard")
    @Builder.Default
    private Boolean showInLeaderboard = true;

    @Column(name = "allow_friend_requests")
    @Builder.Default
    private Boolean allowFriendRequests = true;

    @Column(name = "show_last_active")
    @Builder.Default
    private Boolean showLastActive = true;

    // AI Preferences
    @Column(name = "allow_ai_learning")
    @Builder.Default
    private Boolean allowAILearning = true;

    @Column(name = "proactive_guess_prompts")
    @Builder.Default
    private Boolean proactiveGuessPrompts = true;

    @Column(name = "preferred_ai_personality", length = 50)
    @Builder.Default
    private String preferredAIPersonality = "friendly";

    // Security
    @Column(name = "require_biometric_for_transactions")
    @Builder.Default
    private Boolean requireBiometricForTransactions = false;

    @Column(name = "session_timeout_enabled")
    @Builder.Default
    private Boolean sessionTimeoutEnabled = true;

    @Column(name = "session_timeout_minutes")
    @Builder.Default
    private Integer sessionTimeoutMinutes = 30;
}
