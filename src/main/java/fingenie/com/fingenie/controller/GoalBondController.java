package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.GoalBondMissionClaimResponse;
import fingenie.com.fingenie.dto.GoalBondMissionStateResponse;
import fingenie.com.fingenie.dto.GoalBondSummaryResponse;
import fingenie.com.fingenie.service.GoalBondService;
import fingenie.com.fingenie.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api-prefix}/piggies/{piggyId}/goalbond")
@RequiredArgsConstructor
public class GoalBondController {

    private final GoalBondService goalBondService;

    @GetMapping("/summary")
    public ResponseEntity<GoalBondSummaryResponse> getSummary(@PathVariable Long piggyId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(goalBondService.getSummary(piggyId, accountId));
    }

    @GetMapping("/missions/today")
    public ResponseEntity<GoalBondMissionStateResponse> getTodayMissions(@PathVariable Long piggyId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(goalBondService.getTodayMissionState(piggyId, accountId));
    }

    @PostMapping("/missions/{missionId}/claim")
    public ResponseEntity<GoalBondMissionClaimResponse> claimMission(
            @PathVariable Long piggyId,
            @PathVariable String missionId
    ) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(goalBondService.claimMission(piggyId, accountId, missionId));
    }
}
