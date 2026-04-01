package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlanResponse {
    private String planCode;
    private String title;
    private String description;
    private Long amount;
    private String currency;
    private Integer durationDays;
}
