package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.AnalyticsResponse;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.service.AnalyticsService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Spending analytics and insights")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final EntitlementService entitlementService;

    @GetMapping
    @Operation(summary = "Get analytics", description = "Get comprehensive spending analytics for a date range")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(analyticsService.getAnalytics(accountId, startDate, endDate));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly analytics", description = "Get analytics for a specific month")
    public ResponseEntity<AnalyticsResponse> getMonthlyAnalytics(
            @Parameter(description = "Year") @RequestParam int year,
            @Parameter(description = "Month (1-12)") @RequestParam int month) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        entitlementService.assertFeature(accountId, "analytics.month");
        return ResponseEntity.ok(analyticsService.getMonthlyAnalytics(accountId, year, month));
    }

    @GetMapping("/yearly")
    @Operation(summary = "Get yearly analytics", description = "Get analytics for a full year")
    public ResponseEntity<AnalyticsResponse> getYearlyAnalytics(
            @Parameter(description = "Year") @RequestParam int year) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        entitlementService.assertFeature(accountId, "analytics.year");
        return ResponseEntity.ok(analyticsService.getYearlyAnalytics(accountId, year));
    }

    @GetMapping("/weekly")
    @Operation(summary = "Get weekly analytics", description = "Get analytics for the last 7 days")
    public ResponseEntity<AnalyticsResponse> getWeeklyAnalytics() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(analyticsService.getWeeklyAnalytics(accountId));
    }

    @GetMapping("/current-month")
    @Operation(summary = "Get current month analytics", description = "Get analytics for the current month")
    public ResponseEntity<AnalyticsResponse> getCurrentMonthAnalytics() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        entitlementService.assertFeature(accountId, "analytics.month");
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(analyticsService.getMonthlyAnalytics(accountId, now.getYear(), now.getMonthValue()));
    }

    @GetMapping("/category/{categoryId}/trend")
    @Operation(summary = "Get category trend", description = "Get spending trend for a category over months")
    public ResponseEntity<List<AnalyticsService.CategoryTrendResponse>> getCategoryTrend(
            @PathVariable Long categoryId,
            @Parameter(description = "Number of months to analyze") @RequestParam(defaultValue = "6") int months) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(analyticsService.getCategoryTrends(accountId, categoryId, months));
    }
}
