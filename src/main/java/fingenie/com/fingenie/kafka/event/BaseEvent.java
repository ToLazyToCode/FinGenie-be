package fingenie.com.fingenie.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Base event class for all Kafka events in FinGenie.
 * 
 * All events include:
 * - schema_version: For schema evolution
 * - event_id: Unique identifier (UUID)
 * - event_type: Type of event
 * - timestamp: When event occurred
 * - idempotency_key: For deduplication
 * - correlation_id: For tracing across services
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    @JsonProperty("schema_version")
    private String schemaVersion = "v1";

    @JsonProperty("event_id")
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("event_type")
    private String eventType;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp = Instant.now();

    @JsonProperty("idempotency_key")
    private String idempotencyKey = UUID.randomUUID().toString();

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("source_service")
    private String sourceService = "fingenie-backend";

    public void generateIds() {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
