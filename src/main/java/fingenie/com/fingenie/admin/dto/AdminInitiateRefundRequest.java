package fingenie.com.fingenie.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body for initiating a refund on a payment order.
 *
 * <p>If {@code refundAmount} is omitted the service will refund the full
 * original order amount.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInitiateRefundRequest {

    /**
     * Partial refund amount in VND.
     * Leave {@code null} to refund the full order amount.
     */
    @DecimalMin(value = "1", message = "Refund amount must be at least 1 VND")
    private BigDecimal refundAmount;

    /** Reason for initiating the refund (required). */
    @NotBlank(message = "Reason is required")
    private String reason;
}
