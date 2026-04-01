package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StreakRequest {
    private Long accountId;
    private Integer currentStreak;
    private Integer longestStreak;
    private String lastActiveDate;
}
