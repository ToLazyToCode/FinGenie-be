package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.DashboardResponse;
import fingenie.com.fingenie.service.DashboardAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api-prefix}/experience/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Experience", description = "Aggregated dashboard data for mobile home screen")
public class DashboardExperienceController {

    private final DashboardAggregationService dashboardService;

    @Operation(
        summary = "Get dashboard summary",
        description = "Returns aggregated data: balance, streak, XP, pet mood, AI insight - all in single call"
    )
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
