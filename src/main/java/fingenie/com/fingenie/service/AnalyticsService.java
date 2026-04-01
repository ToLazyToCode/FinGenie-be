package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.AnalyticsResponse;
import fingenie.com.fingenie.dto.AnalyticsResponse.*;
import fingenie.com.fingenie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    /**
     * Get comprehensive analytics for a time period.
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long accountId, LocalDate startDate, LocalDate endDate) {
        Date sqlStartDate = Date.valueOf(startDate);
        Date sqlEndDate = Date.valueOf(endDate);

        // Calculate previous period for comparison
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevStart = startDate.minusDays(daysDiff + 1);
        LocalDate prevEnd = startDate.minusDays(1);
        Date sqlPrevStart = Date.valueOf(prevStart);
        Date sqlPrevEnd = Date.valueOf(prevEnd);

        // Current period summary
        PeriodSummary currentPeriod = buildPeriodSummary(accountId, sqlStartDate, sqlEndDate, startDate, endDate);

        // Previous period summary
        PeriodSummary previousPeriod = buildPeriodSummary(accountId, sqlPrevStart, sqlPrevEnd, prevStart, prevEnd);

        // Category breakdown
        List<CategoryBreakdown> categoryBreakdown = buildCategoryBreakdown(accountId, sqlStartDate, sqlEndDate, currentPeriod.getTotalExpense());

        // Daily trends
        List<DailyTrend> dailyTrend = buildDailyTrend(accountId, sqlStartDate, sqlEndDate);

        // Spending comparison
        SpendingComparison comparison = buildComparison(currentPeriod, previousPeriod);

        // Top merchants
        List<TopMerchant> topMerchants = buildTopMerchants(accountId, sqlStartDate, sqlEndDate);

        return AnalyticsResponse.builder()
                .currentPeriod(currentPeriod)
                .previousPeriod(previousPeriod)
                .categoryBreakdown(categoryBreakdown)
                .dailyTrend(dailyTrend)
                .comparison(comparison)
                .topMerchants(topMerchants)
                .build();
    }

    /**
     * Get monthly analytics (convenience method).
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getMonthlyAnalytics(Long accountId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return getAnalytics(accountId, startDate, endDate);
    }

    /**
     * Get weekly analytics.
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getWeeklyAnalytics(Long accountId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return getAnalytics(accountId, startDate, endDate);
    }

    /**
     * Get yearly analytics.
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getYearlyAnalytics(Long accountId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return getAnalytics(accountId, startDate, endDate);
    }

    /**
     * Get category-specific trends over time.
     */
    @Transactional(readOnly = true)
    public List<CategoryTrendResponse> getCategoryTrends(Long accountId, Long categoryId, int months) {
        List<CategoryTrendResponse> trends = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            Date sqlStart = Date.valueOf(monthStart);
            Date sqlEnd = Date.valueOf(monthEnd);

            BigDecimal expense = transactionRepository.sumExpenseByAccountIdAndDateRange(accountId, sqlStart, sqlEnd);
            Long count = transactionRepository.countByAccountIdAndDateRange(accountId, sqlStart, sqlEnd);

            trends.add(CategoryTrendResponse.builder()
                    .month(monthStart.getMonth().toString())
                    .year(monthStart.getYear())
                    .totalSpent(expense != null ? expense : BigDecimal.ZERO)
                    .transactionCount(count != null ? count.intValue() : 0)
                    .build());
        }

        return trends;
    }

    // ==================== Private Helper Methods ====================

    private PeriodSummary buildPeriodSummary(Long accountId, Date startDate, Date endDate,
                                              LocalDate localStart, LocalDate localEnd) {
        BigDecimal income = transactionRepository.sumIncomeByAccountIdAndDateRange(accountId, startDate, endDate);
        BigDecimal expense = transactionRepository.sumExpenseByAccountIdAndDateRange(accountId, startDate, endDate);
        Long txCount = transactionRepository.countByAccountIdAndDateRange(accountId, startDate, endDate);

        income = income != null ? income : BigDecimal.ZERO;
        expense = expense != null ? expense : BigDecimal.ZERO;
        int count = txCount != null ? txCount.intValue() : 0;

        BigDecimal avgTx = BigDecimal.ZERO;
        if (count > 0) {
            avgTx = expense.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }

        // Find largest expense
        List<Object[]> categories = transactionRepository.sumExpenseByCategory(accountId, startDate, endDate);
        BigDecimal largestExpense = BigDecimal.ZERO;
        String largestCategory = null;
        if (!categories.isEmpty()) {
            Object[] top = categories.get(0);
            largestExpense = (BigDecimal) top[2];
            largestCategory = (String) top[1];
        }

        return PeriodSummary.builder()
                .startDate(localStart)
                .endDate(localEnd)
                .totalIncome(income)
                .totalExpense(expense)
                .netAmount(income.subtract(expense))
                .transactionCount(count)
                .averageTransaction(avgTx)
                .largestExpense(largestExpense)
                .largestExpenseCategory(largestCategory)
                .build();
    }

    private List<CategoryBreakdown> buildCategoryBreakdown(Long accountId, Date startDate, Date endDate,
                                                            BigDecimal totalExpense) {
        List<Object[]> results = transactionRepository.sumExpenseByCategory(accountId, startDate, endDate);
        List<CategoryBreakdown> breakdown = new ArrayList<>();

        for (Object[] row : results) {
            Long categoryId = (Long) row[0];
            String categoryName = (String) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            Long count = (Long) row[3];

            double percentage = 0;
            if (totalExpense != null && totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.divide(totalExpense, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            breakdown.add(CategoryBreakdown.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .amount(amount)
                    .percentage(percentage)
                    .transactionCount(count != null ? count.intValue() : 0)
                    .build());
        }

        return breakdown;
    }

    private List<DailyTrend> buildDailyTrend(Long accountId, Date startDate, Date endDate) {
        List<Object[]> results = transactionRepository.getDailyTotals(accountId, startDate, endDate);
        List<DailyTrend> trends = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;

        for (Object[] row : results) {
            Date date = (Date) row[0];
            BigDecimal income = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal expense = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            cumulative = cumulative.add(income).subtract(expense);

            trends.add(DailyTrend.builder()
                    .date(date.toLocalDate())
                    .income(income)
                    .expense(expense)
                    .cumulative(cumulative)
                    .build());
        }

        return trends;
    }

    private SpendingComparison buildComparison(PeriodSummary current, PeriodSummary previous) {
        BigDecimal currentExpense = current.getTotalExpense();
        BigDecimal previousExpense = previous.getTotalExpense();

        BigDecimal absoluteChange = currentExpense.subtract(previousExpense);
        double percentageChange = 0;
        String trend = "STABLE";
        String insight;

        if (previousExpense.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = absoluteChange.divide(previousExpense, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        if (percentageChange > 5) {
            trend = "UP";
            insight = String.format("Spending increased by %.1f%% compared to last period", percentageChange);
        } else if (percentageChange < -5) {
            trend = "DOWN";
            insight = String.format("Great job! Spending decreased by %.1f%% compared to last period", Math.abs(percentageChange));
        } else {
            trend = "STABLE";
            insight = "Spending is consistent with last period";
        }

        return SpendingComparison.builder()
                .percentageChange(percentageChange)
                .absoluteChange(absoluteChange)
                .trend(trend)
                .insight(insight)
                .build();
    }

    private List<TopMerchant> buildTopMerchants(Long accountId, Date startDate, Date endDate) {
        List<Object[]> results = transactionRepository.getTopMerchants(accountId, startDate, endDate);
        List<TopMerchant> merchants = new ArrayList<>();

        int limit = Math.min(results.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = results.get(i);
            String merchantName = (String) row[0];
            BigDecimal totalSpent = (BigDecimal) row[1];
            Long count = (Long) row[2];
            String categoryName = (String) row[3];

            if (merchantName != null && !merchantName.isEmpty()) {
                merchants.add(TopMerchant.builder()
                        .merchantName(merchantName)
                        .totalSpent(totalSpent)
                        .transactionCount(count != null ? count.intValue() : 0)
                        .categoryName(categoryName)
                        .build());
            }
        }

        return merchants;
    }

    // ==================== Response DTOs ====================

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class CategoryTrendResponse {
        private String month;
        private int year;
        private BigDecimal totalSpent;
        private int transactionCount;
    }
}
