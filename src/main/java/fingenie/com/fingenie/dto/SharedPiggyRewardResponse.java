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
public class SharedPiggyRewardResponse {
    private Long unlockId;
    private Long piggyId;
    private String milestoneKey;
    private RewardStatus status;
    private Long goalBondProgressAtUnlock;
    private Long goalBondTargetAtUnlock;
    private Timestamp unlockedAt;
    private Timestamp expiresAt;

    private Long rewardId;
    private String code;
    private String title;
    private String description;
    private RewardCatalogItem.Category category;
    private String goalThemeTags;
    private String partnerName;
    private String partnerMetadata;
    private String imageUrl;
    private String termsUrl;
}
