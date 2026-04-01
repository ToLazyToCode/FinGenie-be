package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_achievement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "achievement_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "progress_value", nullable = false)
    @Builder.Default
    private Integer progressValue = 0;

    @Column(name = "is_unlocked", nullable = false)
    @Builder.Default
    private Boolean isUnlocked = false;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "is_claimed", nullable = false)
    @Builder.Default
    private Boolean isClaimed = false;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
}
