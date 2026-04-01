package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalBondMissionStateResponse {
    private Long piggyId;
    private String dayKey;
    private List<MissionState> missions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionState {
        private String missionId;
        private Integer requiredCount;
        private Integer progressCount;
        private Long rewardGoalBond;
        private Boolean claimable;
        private Boolean completed;
        private Timestamp claimedAt;
    }
}
