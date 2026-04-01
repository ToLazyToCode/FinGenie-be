package fingenie.com.fingenie.entity;

/**
 * Lifecycle states for a {@link Refund} record.
 *
 * <pre>
 * PENDING  → APPROVED
 *         → REJECTED
 * </pre>
 */
public enum RefundStatus {
    /** Refund has been created and is awaiting admin review. */
    PENDING,

    /** Refund was reviewed and approved by an admin. */
    APPROVED,

    /** Refund was reviewed and rejected by an admin. */
    REJECTED
}
