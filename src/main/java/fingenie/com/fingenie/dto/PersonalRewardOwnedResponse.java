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
public class PersonalRewardOwnedResponse {
    private Long redemptionId;
    private Long rewardId;
    private String code;
    private String title;
    private String description;
    private RewardCatalogItem.Category category;
    private RewardStatus status;
    private Long finPointCost;
    private Timestamp claimedAt;
    private Timestamp redeemedAt;
    private Timestamp expiresAt;
}
