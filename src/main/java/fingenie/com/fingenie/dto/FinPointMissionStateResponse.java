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
public class FinPointMissionStateResponse {
    private String dayKey;
    private Integer xpToday;
    private Long finPointToday;
    private List<MissionRewardState> missions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionRewardState {
        private String missionId;
        private Boolean completed;
        private Integer xpReward;
        private Long finPointReward;
        private Timestamp claimedAt;
    }
}
