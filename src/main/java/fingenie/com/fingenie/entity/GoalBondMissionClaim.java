package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;

@Entity
@Table(
        name = "goalbond_mission_claim",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_goalbond_claim_idempotency_key", columnNames = "idempotency_key"),
                @UniqueConstraint(
                        name = "uk_goalbond_claim_member_mission_day",
                        columnNames = {"piggy_id", "account_id", "mission_id", "mission_day"}
                )
        },
        indexes = {
                @Index(name = "idx_goalbond_claim_piggy_day", columnList = "piggy_id, mission_day"),
                @Index(name = "idx_goalbond_claim_account_day", columnList = "account_id, mission_day")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalBondMissionClaim extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piggy_id", nullable = false)
    private PiggyBank piggyBank;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "mission_id", nullable = false, length = 80)
    private String missionId;

    @Column(name = "mission_day", nullable = false)
    private LocalDate missionDay;

    @Column(name = "reward_amount", nullable = false)
    private Long rewardAmount;

    @Column(name = "progress_snapshot")
    private Integer progressSnapshot;

    @Column(name = "idempotency_key", nullable = false, length = 220)
    private String idempotencyKey;
}
