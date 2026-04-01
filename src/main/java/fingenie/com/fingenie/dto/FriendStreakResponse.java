package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FriendStreakResponse {
    private Long friendStreakId;
    private Long friendshipId;
    private Integer currentStreak;
    private LocalDate lastActiveDate;
}
