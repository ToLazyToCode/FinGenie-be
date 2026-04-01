package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(
        name = "notification_device_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_device_token_account_token",
                        columnNames = {"account_id", "device_token"}
                )
        },
        indexes = {
                @Index(name = "idx_notification_device_token_account", columnList = "account_id"),
                @Index(name = "idx_notification_device_token_enabled", columnList = "enabled")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeviceToken extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "device_token", nullable = false, length = 600)
    private String deviceToken;

    @Column(name = "platform", length = 32)
    private String platform;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "last_seen_at", nullable = false)
    private Timestamp lastSeenAt;
}
