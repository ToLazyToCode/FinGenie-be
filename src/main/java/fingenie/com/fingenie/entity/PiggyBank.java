package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "piggy_bank")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PiggyBank extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, unique = true)
    private Wallet wallet;

    @Column(name = "goal_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal goalAmount;

    @Column(name = "lock_until")
    private Date lockUntil;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "withdrawal_penalty_rate", precision = 5, scale = 2)
    private BigDecimal withdrawalPenaltyRate;

    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private boolean isShared = false;
}
