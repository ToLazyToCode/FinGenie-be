package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Financial KPI summary for the admin dashboard's financial management section.
 *
 * <ul>
 *   <li>volume24h / 7d / 30d – sum of PAID subscription orders in each window</li>
 *   <li>totalRevenue          – all-time PAID revenue</li>
 *   <li>averageTransactionAmount – average of PAID order amounts</li>
 *   <li>successRate / refundRate – percentages (0-100)</li>
 *   <li>totalTransactions     – count of all payment orders</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFinancialSummaryResponse {

    /** Sum of paid orders in the last 24 hours (VND). */
    private BigDecimal volume24h;

    /** Sum of paid orders in the last 7 days (VND). */
    private BigDecimal volume7d;

    /** Sum of paid orders in the last 30 days (VND). */
    private BigDecimal volume30d;

    /** Average amount of all paid orders (VND). */
    private BigDecimal averageTransactionAmount;

    /** Percentage of transactions that were refunded: (approved refunds / total orders) × 100. */
    private Double refundRate;

    /** Percentage of payment orders that succeeded (PAID): (paid / total) × 100. */
    private Double successRate;

    /** Total number of payment orders (all statuses). */
    private Long totalTransactions;

    /** All-time revenue from PAID orders (VND). */
    private BigDecimal totalRevenue;
}
