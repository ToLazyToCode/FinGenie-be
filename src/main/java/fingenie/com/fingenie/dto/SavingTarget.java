package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingTarget {

    private TargetType type;
    private Long id;
    private String title;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private Instant deadline;
    private BigDecimal requiredMonthly;
    private String notes;

    @Builder.Default
    private Map<String, Object> metadata = Collections.emptyMap();

    public enum TargetType {
        GOAL,
        PIGGY
    }
}
