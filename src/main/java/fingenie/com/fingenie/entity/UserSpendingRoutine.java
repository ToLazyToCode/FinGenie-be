package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_spending_routine")
@Getter
@Setter
public class UserSpendingRoutine extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "routine_type", nullable = false, length = 20)
    private String routineType; // DAILY, WEEKLY, MONTHLY, EVENT_BASED

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "typical_amount", precision = 19, scale = 2)
    private BigDecimal typicalAmount;

    @Column(name = "confidence_score", nullable = false, precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.50")
    private BigDecimal confidenceScore = new BigDecimal("0.50");

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "frequency_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer frequencyCount = 0;

    @Column(name = "average_interval_days", nullable = false, precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 1.00")
    private BigDecimal averageIntervalDays = BigDecimal.ONE;

    @Column(name = "time_pattern", length = 100)
    private String timePattern; // e.g., "morning", "evening", "weekend"

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true;
}
