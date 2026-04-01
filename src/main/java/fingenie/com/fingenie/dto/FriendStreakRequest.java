package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendStreakRequest {
    private Long friendshipId;
    private Integer currentStreak;
}
