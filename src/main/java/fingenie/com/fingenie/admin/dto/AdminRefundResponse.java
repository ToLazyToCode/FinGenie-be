package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin-facing view of a single refund record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundResponse {

    /** Refund record ID. */
    private Long id;

    /** ID of the PaymentOrder being refunded. */
    private Long transactionId;

    /** Original order amount (VND). */
    private BigDecimal originalAmount;

    /** Requested refund amount (VND) – may be partial. */
    private BigDecimal refundAmount;

    /** Reason provided by the admin who initiated the refund. */
    private String reason;

    /** Refund status: {@code PENDING | APPROVED | REJECTED}. */
    private String status;

    /** When the refund request was created. */
    private LocalDateTime requestedAt;

    /** When the refund was approved or rejected ({@code null} if still pending). */
    private LocalDateTime processedAt;

    /** Email of the admin who processed the refund ({@code null} if still pending). */
    private String processedBy;

    /** Optional notes left by the approving/rejecting admin. */
    private String notes;
}
