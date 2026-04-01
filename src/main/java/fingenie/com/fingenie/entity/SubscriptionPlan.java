package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "subscription_plan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_subscription_plan_code", columnNames = "plan_code")
        },
        indexes = {
                @Index(name = "idx_subscription_plan_active", columnList = "is_active"),
                @Index(name = "idx_subscription_plan_sort", columnList = "sort_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "plan_code", nullable = false, length = 60)
    private String planCode;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
