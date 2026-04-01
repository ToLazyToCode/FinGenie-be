package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StreakResponse {
    private Long streakId;
    private Long accountId;
    private Integer currentStreak;
    private Integer longestStreak;
    private String lastActiveDate;
}

