package fingenie.com.fingenie.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for rejecting a pending refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundRejectRequest {

    /** ID of the refund to reject. */
    private Long refundId;

    /** Reason for rejection (required). */
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
