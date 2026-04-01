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
        name = "payment_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order_code", columnNames = "order_code")
        },
        indexes = {
                @Index(name = "idx_payment_order_account_created", columnList = "account_id, created_at"),
                @Index(name = "idx_payment_order_status", columnList = "status"),
                @Index(name = "idx_payment_order_gateway_ref", columnList = "gateway_order_ref"),
                @Index(name = "idx_payment_order_gateway_txn", columnList = "gateway_transaction_ref")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder extends BaseEntity {

    @Column(name = "order_code", nullable = false, length = 80)
    private String orderCode;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false, length = 20)
    private PaymentGateway gateway;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;

    @Column(name = "gateway_order_ref", length = 120)
    private String gatewayOrderRef;

    @Column(name = "gateway_transaction_ref", length = 160)
    private String gatewayTransactionRef;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "raw_init_payload", columnDefinition = "TEXT")
    private String rawInitPayload;

    @Column(name = "paid_at")
    private Timestamp paidAt;

    @Column(name = "expires_at")
    private Timestamp expiresAt;
}
