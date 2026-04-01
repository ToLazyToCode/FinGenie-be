package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "shared_piggy_reward_unlock",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shared_piggy_milestone",
                        columnNames = {"piggy_id", "milestone_key"}
                )
        },
        indexes = {
                @Index(name = "idx_shared_reward_piggy_status", columnList = "piggy_id, status"),
                @Index(name = "idx_shared_reward_reward", columnList = "reward_catalog_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPiggyRewardUnlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piggy_id", nullable = false)
    private PiggyBank piggyBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_catalog_id", nullable = false)
    private RewardCatalogItem rewardCatalog;

    @Column(name = "milestone_key", nullable = false, length = 100)
    private String milestoneKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RewardStatus status = RewardStatus.AVAILABLE;

    @Column(name = "goalbond_progress_at_unlock", nullable = false)
    private Long goalBondProgressAtUnlock;

    @Column(name = "goalbond_target_at_unlock", nullable = false)
    private Long goalBondTargetAtUnlock;

    @Column(name = "unlocked_at", nullable = false)
    private Timestamp unlockedAt;

    @Column(name = "expires_at")
    private Timestamp expiresAt;
}
