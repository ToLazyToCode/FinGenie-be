package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationProfileDto {
    private Long accountId;
    private Integer xp;
    private Integer level;
}
