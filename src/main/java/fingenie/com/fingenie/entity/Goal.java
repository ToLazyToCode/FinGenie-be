package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "piggy_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal extends BaseEntity {

    private Long accountId;

    private String title;

    private String iconUrl;

    private BigDecimal targetAmount;

    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10, nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    private Instant deadline;
    
    private Instant completedAt;
    
    @Builder.Default
    private boolean isCompleted = false;

    public boolean checkCompleted() {
        return currentAmount != null && targetAmount != null && 
               currentAmount.compareTo(targetAmount) >= 0;
    }
    
    public BigDecimal getRemainingAmount() {
        if (targetAmount == null || currentAmount == null) return BigDecimal.ZERO;
        BigDecimal remaining = targetAmount.subtract(currentAmount);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }
    
    public int getProgressPercent() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) == 0) return 0;
        return currentAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }
}
