package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "piggy_bank_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PiggyBankMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piggy_id", nullable = false)
    private PiggyBank piggyBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.CONTRIBUTOR;

    @Column(name = "share_weight", nullable = false)
    @Builder.Default
    private Integer shareWeight = 1;

    @Column(name = "monthly_commitment", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal monthlyCommitment = BigDecimal.ZERO;

    @Getter
    public enum MemberRole {
        OWNER("owner"),
        CONTRIBUTOR("contributor");

        private final String value;

        MemberRole(String value) {
            this.value = value;
        }

    }

}
