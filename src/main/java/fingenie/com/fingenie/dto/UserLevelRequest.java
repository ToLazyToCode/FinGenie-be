package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserLevelRequest {

    private Long accountId;
    private Integer currentLevel;
    private Integer currentXp;
    private Integer lifetimeXp;
}
