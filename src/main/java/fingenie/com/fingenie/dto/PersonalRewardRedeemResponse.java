package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.RewardCatalogItem;
import fingenie.com.fingenie.entity.RewardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRewardRedeemResponse {
    private Long rewardId;
    private String code;
    private String title;
    private RewardCatalogItem.Category category;
    private boolean granted;
    private RewardStatus status;
    private Long finPointCost;
    private Long balanceAfter;
    private Long redemptionId;
    private Timestamp claimedAt;
}
