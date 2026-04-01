package fingenie.com.fingenie.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * User Event - Tracks user activities for feature computation.
 * 
 * Event types:
 * - USER_LOGIN: User logged in
 * - USER_LOGOUT: User logged out
 * - PROFILE_UPDATED: Profile information changed
 * - SETTINGS_CHANGED: User settings modified
 * - WALLET_CREATED: New wallet added
 * - BUDGET_SET: Budget configured
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent {

    @JsonProperty("schema_version")
    @Builder.Default
    private String schemaVersion = "v1";

    @JsonProperty("event_id")
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @JsonProperty("event_type")
    private String eventType;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("idempotency_key")
    @Builder.Default
    private String idempotencyKey = UUID.randomUUID().toString();

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("source_service")
    @Builder.Default
    private String sourceService = "fingenie-backend";

    // === User Context ===
    
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    // === Event-specific Payload ===
    
    private Map<String, Object> payload;

    // === Factory Methods ===

    public static UserEvent login(Long userId, Long accountId, String deviceId, String ipAddress) {
        return UserEvent.builder()
                .eventType("USER_LOGIN")
                .userId(userId)
                .accountId(accountId)
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .build();
    }

    public static UserEvent logout(Long userId, Long accountId) {
        return UserEvent.builder()
                .eventType("USER_LOGOUT")
                .userId(userId)
                .accountId(accountId)
                .build();
    }

    public static UserEvent profileUpdated(Long userId, Map<String, Object> changes) {
        return UserEvent.builder()
                .eventType("PROFILE_UPDATED")
                .userId(userId)
                .payload(changes)
                .build();
    }

    public static UserEvent budgetSet(Long userId, Long categoryId, BigDecimal amount) {
        return UserEvent.builder()
                .eventType("BUDGET_SET")
                .userId(userId)
                .payload(Map.of(
                        "category_id", categoryId,
                        "amount", amount
                ))
                .build();
    }
}
