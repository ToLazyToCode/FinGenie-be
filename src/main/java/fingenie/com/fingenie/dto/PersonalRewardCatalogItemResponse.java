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
public class PersonalRewardCatalogItemResponse {
    private Long rewardId;
    private String code;
    private String title;
    private String description;
    private RewardCatalogItem.Category category;
    private Long pointCost;
    private String goalThemeTags;
    private String partnerName;
    private String partnerMetadata;
    private String imageUrl;
    private String termsUrl;
    private Timestamp expiresAt;
    private boolean owned;
    private RewardStatus ownedStatus;
    private boolean canRedeem;
}
