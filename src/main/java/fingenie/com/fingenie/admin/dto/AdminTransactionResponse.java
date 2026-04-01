package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detailed representation of a payment-order transaction for the admin financial view.
 *
 * <p>Maps to the {@code PaymentOrder} entity – subscription payments routed through
 * PayOS / VNPay gateways.</p>
 *
 * <ul>
 *   <li>{@code type}     – always {@code "SUBSCRIPTION"} for payment orders</li>
 *   <li>{@code category} – subscription plan title</li>
 *   <li>{@code gateway}  – {@code "PAYOS"} or {@code "VNPAY"}</li>
 *   <li>{@code status}   – {@code PENDING | REDIRECTED | PAID | FAILED | EXPIRED | CANCELLED}</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTransactionResponse {

    private Long id;

    /** The account (user) that initiated the payment. */
    private Long userId;

    /** Email of the paying user. */
    private String userEmail;

    /** Order amount in VND. */
    private BigDecimal amount;

    /** Transaction type – {@code "SUBSCRIPTION"}. */
    private String type;

    /** Subscription plan title (e.g. "Premium Monthly"). */
    private String category;

    /** Payment order status (PAID, FAILED, PENDING, …). */
    private String status;

    /** Payment gateway name (PAYOS / VNPAY). */
    private String gateway;

    /** Not applicable for payment orders – always {@code null}. */
    private String walletName;

    /** Internal order code used with the gateway. */
    private String description;

    /** When the order was paid / last updated (paidAt, falls back to createdAt). */
    private LocalDateTime timestamp;

    /** Record creation time. */
    private LocalDateTime createdAt;
}
