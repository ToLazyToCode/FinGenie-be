package fingenie.com.fingenie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User notification preferences.
 */
@Entity
@Table(name = "notification_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "budget_alerts", nullable = false)
    @Builder.Default
    private Boolean budgetAlerts = true;

    @Column(name = "streak_reminders", nullable = false)
    @Builder.Default
    private Boolean streakReminders = true;

    @Column(name = "guess_prompts", nullable = false)
    @Builder.Default
    private Boolean guessPrompts = true;

    @Column(name = "goal_progress", nullable = false)
    @Builder.Default
    private Boolean goalProgress = true;

    @Column(name = "friend_activity", nullable = false)
    @Builder.Default
    private Boolean friendActivity = true;

    @Column(name = "achievement_unlocks", nullable = false)
    @Builder.Default
    private Boolean achievementUnlocks = true;

    @Column(name = "daily_summary", nullable = false)
    @Builder.Default
    private Boolean dailySummary = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
