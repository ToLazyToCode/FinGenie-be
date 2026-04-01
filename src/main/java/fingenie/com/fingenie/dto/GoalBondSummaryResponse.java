package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalBondSummaryResponse {
    private Long piggyId;
    private Long currentProgress;
    private Long targetProgress;
    private Integer progressPercent;
    private String status;
}
