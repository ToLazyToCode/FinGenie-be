package fingenie.com.fingenie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String tier;
    private String category;
    private Integer xpReward;
    private Integer targetValue;
    private Boolean isHidden;

    // User-specific fields
    private Integer progressValue;
    private Double progressPercentage;
    private Boolean isUnlocked;
    private Boolean isClaimed;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime unlockedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime claimedAt;
}
