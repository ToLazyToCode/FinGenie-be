package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "saving_contribution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingContribution extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20, nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false)
    @Builder.Default
    private Source source = Source.MANUAL;

    public enum TargetType {
        GOAL,
        PIGGY
    }

    public enum Source {
        MANUAL,
        AUTO
    }
}
