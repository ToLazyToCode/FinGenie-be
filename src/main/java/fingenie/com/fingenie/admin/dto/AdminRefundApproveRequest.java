package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for approving a pending refund.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundApproveRequest {

    /** ID of the refund to approve. */
    private Long refundId;

    /** Optional notes / memo from the approving admin. */
    private String notes;
}
