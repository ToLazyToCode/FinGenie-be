package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserLevelResponse {

    private Long accountId;
    private Integer currentLevel;
    private Integer currentXp;
    private Integer lifetimeXp;
    private LocalDateTime updatedAt;
}
