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
public class GoalBondMissionClaimResponse {
    private Long piggyId;
    private String missionId;
    private String dayKey;
    private Boolean granted;
    private Long rewardAwarded;
    private Long currentProgress;
    private Long targetProgress;
    private String status;
    private Timestamp claimedAt;
}
