package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.SavingContribution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingContributionResponse {
    private Long id;
    private Long accountId;
    private SavingContribution.TargetType targetType;
    private Long targetId;
    private BigDecimal amount;
    private SavingContribution.Source source;
    private Timestamp createdAt;
}
