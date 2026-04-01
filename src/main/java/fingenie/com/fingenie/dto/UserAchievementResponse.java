package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserAchievementResponse {

    private Long userAchievementId;
    private Long accountId;
    private Long achievementId;
    private Integer progressValue;
    private Boolean isUnlocked;
    private LocalDateTime unlockedAt;
    private LocalDateTime createdAt;
}
