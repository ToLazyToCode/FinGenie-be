package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PiggyGoalRequest {
    private String title;
    private String iconUrl;
    private BigDecimal targetAmount;
    private Instant deadline;
}
