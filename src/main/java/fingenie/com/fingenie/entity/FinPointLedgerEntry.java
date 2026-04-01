package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
        name = "finpoint_ledger",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_finpoint_idempotency_key", columnNames = "idempotency_key"),
                @UniqueConstraint(name = "uk_finpoint_account_mission_day", columnNames = {"account_id", "mission_id", "mission_day"})
        },
        indexes = {
                @Index(name = "idx_finpoint_account_created_at", columnList = "account_id, created_at"),
                @Index(name = "idx_finpoint_account_mission_day", columnList = "account_id, mission_day")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinPointLedgerEntry extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private SourceType sourceType;

    @Column(name = "source_ref_type", length = 60)
    private String sourceRefType;

    @Column(name = "source_ref_id", length = 120)
    private String sourceRefId;

    @Column(name = "reason", nullable = false, length = 160)
    private String reason;

    @Column(name = "mission_id", length = 60)
    private String missionId;

    @Column(name = "mission_day")
    private LocalDate missionDay;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    public enum SourceType {
        DAILY_MISSION,
        CAMPAIGN_BONUS,
        REFERRAL_BONUS,
        ADMIN_ADJUSTMENT,
        VOUCHER_REDEMPTION,
        SHARED_REWARD
    }
}
