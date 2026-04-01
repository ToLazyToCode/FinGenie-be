package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "goalbond_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_goalbond_progress_piggy", columnNames = "piggy_id")
        },
        indexes = {
                @Index(name = "idx_goalbond_progress_piggy", columnList = "piggy_id"),
                @Index(name = "idx_goalbond_progress_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalBondProgress extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piggy_id", nullable = false, unique = true)
    private PiggyBank piggyBank;

    @Column(name = "current_progress", nullable = false)
    @Builder.Default
    private Long currentProgress = 0L;

    @Column(name = "target_progress", nullable = false)
    @Builder.Default
    private Long targetProgress = 100L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.IN_PROGRESS;

    public enum Status {
        IN_PROGRESS,
        TARGET_REACHED
    }
}
