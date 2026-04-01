package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserAchievementRequest {

    private Long accountId;
    private Long achievementId;
    private Integer progressValue;
    private Boolean isUnlocked;
}
