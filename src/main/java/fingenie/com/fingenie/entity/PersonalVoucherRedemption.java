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
        name = "personal_voucher_redemption",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_personal_voucher_account_reward", columnNames = {"account_id", "reward_catalog_id"}),
                @UniqueConstraint(name = "uk_personal_voucher_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_personal_voucher_account_status", columnList = "account_id, status"),
                @Index(name = "idx_personal_voucher_reward", columnList = "reward_catalog_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalVoucherRedemption extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_catalog_id", nullable = false)
    private RewardCatalogItem rewardCatalog;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RewardStatus status = RewardStatus.CLAIMED;

    @Column(name = "finpoint_cost", nullable = false)
    private Long finPointCost;

    @Column(name = "finpoint_ledger_entry_id")
    private Long finPointLedgerEntryId;

    @Column(name = "idempotency_key", nullable = false, length = 220)
    private String idempotencyKey;

    @Column(name = "claimed_at", nullable = false)
    private Timestamp claimedAt;

    @Column(name = "redeemed_at")
    private Timestamp redeemedAt;

    @Column(name = "expires_at")
    private Timestamp expiresAt;
}
