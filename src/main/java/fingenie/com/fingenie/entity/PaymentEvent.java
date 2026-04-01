package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(
        name = "payment_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_event_hash", columnNames = "event_hash")
        },
        indexes = {
                @Index(name = "idx_payment_event_order", columnList = "payment_order_id"),
                @Index(name = "idx_payment_event_gateway_type", columnList = "gateway, event_type"),
                @Index(name = "idx_payment_event_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_order_id")
    private PaymentOrder paymentOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false, length = 20)
    private PaymentGateway gateway;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private PaymentEventType eventType;

    @Column(name = "event_hash", nullable = false, length = 80)
    private String eventHash;

    @Column(name = "gateway_event_ref", length = 160)
    private String gatewayEventRef;

    @Column(name = "signature", length = 600)
    private String signature;

    @Column(name = "signature_valid", nullable = false)
    @Builder.Default
    private boolean signatureValid = false;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private boolean processed = false;

    @Column(name = "processing_result", length = 260)
    private String processingResult;

    @Column(name = "processed_at")
    private Timestamp processedAt;
}
