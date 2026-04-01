package fingenie.com.fingenie.admin.controller;

import fingenie.com.fingenie.admin.dto.AdminDashboardChartsResponse;
import fingenie.com.fingenie.admin.dto.AdminDashboardStatsResponse;
import fingenie.com.fingenie.admin.dto.AdminRecentTransactionResponse;
import fingenie.com.fingenie.admin.dto.AdminSystemHealthResponse;
import fingenie.com.fingenie.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api-prefix}/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * GET /api/v1/admin/dashboard/stats
     * Returns KPI counters: active users, new users today, total income, total transactions, review stats.
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getStats() {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }

    /**
     * GET /api/v1/admin/dashboard/charts
     * Returns 30-day growth data suitable for line/bar charts.
     */
    @GetMapping("/charts")
    public ResponseEntity<AdminDashboardChartsResponse> getCharts() {
        return ResponseEntity.ok(adminDashboardService.getCharts());
    }

    /**
     * GET /api/v1/admin/dashboard/recent-transactions?limit=10
     * Returns the most recent transactions across all accounts.
     */
    @GetMapping("/recent-transactions")
    public ResponseEntity<List<AdminRecentTransactionResponse>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminDashboardService.getRecentTransactions(limit));
    }

    /**
     * GET /api/v1/admin/dashboard/system-health
     * Returns JVM heap usage, thread count, DB status, and uptime.
     */
    @GetMapping("/system-health")
    public ResponseEntity<AdminSystemHealthResponse> getSystemHealth() {
        return ResponseEntity.ok(adminDashboardService.getSystemHealth());
    }
}
