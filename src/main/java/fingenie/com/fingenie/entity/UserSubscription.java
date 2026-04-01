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
        name = "user_subscription",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_subscription_account", columnNames = "account_id")
        },
        indexes = {
                @Index(name = "idx_user_subscription_status", columnList = "status"),
                @Index(name = "idx_user_subscription_end", columnList = "ends_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserSubscriptionStatus status = UserSubscriptionStatus.ACTIVE;

    @Column(name = "starts_at", nullable = false)
    private Timestamp startsAt;

    @Column(name = "ends_at", nullable = false)
    private Timestamp endsAt;

    @Column(name = "last_payment_order_code", length = 80)
    private String lastPaymentOrderCode;
}
