package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notification entity for in-app notifications.
 */
@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_notification_account_unread", columnList = "account_id, is_read"),
    @Index(name = "idx_notification_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "type", length = 50, nullable = false)
    private String type; // BUDGET_WARNING, STREAK_REMINDER, ACHIEVEMENT, GOAL_PROGRESS, FRIEND_REQUEST, AI_GUESS

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "action_type", length = 50)
    private String actionType; // NAVIGATE_TO, OPEN_MODAL, DEEP_LINK

    @Column(name = "action_data")
    private String actionData; // JSON: {"screen": "PiggyDetail", "params": {"id": 123}}

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0; // Higher = more important

    public void markRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
