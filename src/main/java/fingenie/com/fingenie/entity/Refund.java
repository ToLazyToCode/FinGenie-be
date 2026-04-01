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
import java.time.LocalDateTime;

/**
 * Tracks admin-initiated refund requests against a {@link PaymentOrder}.
 *
 * <h3>Refund workflow</h3>
 * <ol>
 *   <li>Admin calls POST /api/v1/admin/transactions/{transactionId}/refund →
 *       {@code Refund} created with {@code status = PENDING}.</li>
 *   <li>Admin reviews the pending list via GET /api/v1/admin/refunds/pending.</li>
 *   <li>Admin approves or rejects via the approve / reject endpoints.</li>
 * </ol>
 *
 * <p>JPA DDL-auto=update will create the {@code refund} table automatically.</p>
 */
@Entity
@Table(
        name = "refund",
        indexes = {
                @Index(name = "idx_refund_payment_order", columnList = "payment_order_id"),
                @Index(name = "idx_refund_status",        columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund extends BaseEntity {

    /**
     * FK to the {@link PaymentOrder} being refunded.
     * Stored as a plain Long (not a @ManyToOne) to mirror the pattern used
     * in {@link PaymentOrder#accountId}.
     */
    @Column(name = "payment_order_id", nullable = false)
    private Long paymentOrderId;

    /** Original order amount in VND (copied at refund-creation time). */
    @Column(name = "original_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal originalAmount;

    /**
     * Requested refund amount in VND.
     * May be less than {@code originalAmount} for partial refunds.
     */
    @Column(name = "refund_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal refundAmount;

    /** Reason provided by the admin who initiated the refund. */
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    /** When this refund request was created. */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /** When an admin approved or rejected this refund ({@code null} while PENDING). */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Email of the admin who approved / rejected the refund.
     * Set from {@code SecurityContextHolder} at processing time.
     */
    @Column(name = "processed_by", length = 255)
    private String processedBy;

    /** Optional admin notes recorded at approval / rejection time. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
