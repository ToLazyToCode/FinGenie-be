package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Operational status and analytics for a single payment gateway.
 *
 * <p>Status values:
 * <ul>
 *   <li>{@code UP}       – gateway has recent successful transactions</li>
 *   <li>{@code DEGRADED} – gateway has recent transactions but success rate is low (&lt; 80%)</li>
 *   <li>{@code DOWN}     – no recent transactions found for this gateway</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentGatewayResponse {

    /** Gateway name (e.g. {@code "PAYOS"}, {@code "VNPAY"}). */
    private String gatewayName;

    /** Operational status: {@code UP | DEGRADED | DOWN}. */
    private String status;

    /** Total payment orders routed through this gateway. */
    private Long totalTransactions;

    /** Orders with status {@code PAID}. */
    private Long successfulTransactions;

    /** Orders with status {@code FAILED}. */
    private Long failedTransactions;

    /** {@code successfulTransactions / totalTransactions × 100}. */
    private Double successRate;

    /** Sum of all PAID order amounts through this gateway (VND). */
    private BigDecimal totalVolume;

    /**
     * Average response time in milliseconds.
     * Currently mocked at {@code 0} – a real implementation would need
     * gateway latency tracking in the PaymentEvent log.
     */
    private Long averageResponseTime;

    /** Timestamp of the last health-check evaluation. */
    private LocalDateTime lastHealthCheckAt;
}
