package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "shared_piggy_invitation",
        indexes = {
                @Index(name = "idx_shared_piggy_invitee_status", columnList = "invitee_id, status"),
                @Index(name = "idx_shared_piggy_inviter_status", columnList = "inviter_id, status"),
                @Index(name = "idx_shared_piggy_wallet_status", columnList = "wallet_id, status"),
                @Index(name = "idx_shared_piggy_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedPiggyInvitation extends BaseEntity {

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    @Column(name = "invitee_id", nullable = false)
    private Long inviteeId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "piggy_title", nullable = false)
    private String piggyTitle;

    @Column(name = "goal_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal goalAmount;

    @Column(name = "lock_until")
    private Date lockUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "created_piggy_id")
    private Long createdPiggyId;

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED,
        EXPIRED,
        CANCELLED
    }
}
