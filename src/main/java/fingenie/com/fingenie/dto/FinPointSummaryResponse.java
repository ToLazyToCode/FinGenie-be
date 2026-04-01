package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinPointSummaryResponse {
    private Long accountId;
    private Long balance;
    private Long todayEarned;
    private Long lifetimeEarned;
}
