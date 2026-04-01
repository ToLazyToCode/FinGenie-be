package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Budget entity for tracking spending limits per category or total.
 */
@Entity
@Table(name = "budget",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "category_id", "period_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // null for total budget

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "alert_threshold")
    @Builder.Default
    private Integer alertThreshold = 80;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notify_on_exceed", nullable = false)
    @Builder.Default
    private Boolean notifyOnExceed = true;

    @Column(name = "notify_on_warning", nullable = false)
    @Builder.Default
    private Boolean notifyOnWarning = true;

    @Column(name = "rollover_excess", nullable = false)
    @Builder.Default
    private Boolean rolloverExcess = false;

    @Column(name = "notes")
    private String notes;

    public enum PeriodType {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
    
    public boolean isTotalBudget() {
        return category == null;
    }
}
