package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    /** Total number of active (non-deleted) accounts */
    private long totalActiveUsers;

    /** New accounts registered today */
    private long newUsersToday;

    /** Total income across all transactions (positive amounts) */
    private BigDecimal totalIncome;

    /** Total number of transactions in the system */
    private long totalTransactions;

    /** Total reviews across statuses */
    private long totalReviews;

    /** Reviews waiting for moderation */
    private long pendingReviews;
}
