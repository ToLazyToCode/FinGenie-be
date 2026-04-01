package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.FinPointHistoryPageResponse;
import fingenie.com.fingenie.dto.FinPointMissionClaimResponse;
import fingenie.com.fingenie.dto.FinPointMissionStateResponse;
import fingenie.com.fingenie.dto.FinPointSummaryResponse;
import fingenie.com.fingenie.service.FinPointService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api-prefix}/finpoints")
@RequiredArgsConstructor
@Tag(name = "FinPoint", description = "Personal FinPoint balance, history, and daily mission rewards")
public class FinPointController {

    private final FinPointService finPointService;

    @GetMapping("/summary")
    @Operation(summary = "Get FinPoint balance summary")
    public ResponseEntity<FinPointSummaryResponse> getSummary() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(finPointService.getSummary(accountId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get FinPoint history (paged)")
    public ResponseEntity<FinPointHistoryPageResponse> getHistory(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize;
        if (size != null) {
            resolvedSize = size;
        } else if (limit != null) {
            // Backward-compatibility with previous limit-only contract.
            resolvedSize = limit;
        } else {
            resolvedSize = 20;
        }
        return ResponseEntity.ok(finPointService.getHistoryPage(accountId, resolvedPage, resolvedSize));
    }

    @GetMapping("/missions/today")
    @Operation(summary = "Get today's personal mission FinPoint reward state")
    public ResponseEntity<FinPointMissionStateResponse> getTodayMissions() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(finPointService.getTodayMissionState(accountId));
    }

    @PostMapping("/missions/{missionId}/claim")
    @Operation(summary = "Claim FinPoint for today's mission (idempotent)")
    public ResponseEntity<FinPointMissionClaimResponse> claimMission(
            @PathVariable("missionId") String missionId
    ) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(finPointService.claimDailyMission(accountId, missionId));
    }
}
