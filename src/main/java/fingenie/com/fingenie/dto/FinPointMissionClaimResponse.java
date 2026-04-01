package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinPointMissionClaimResponse {
    private String missionId;
    private String dayKey;
    private Boolean granted;
    private Integer xpReward;
    private Long finPointAwarded;
    private Long balance;
    private Timestamp claimedAt;
}
