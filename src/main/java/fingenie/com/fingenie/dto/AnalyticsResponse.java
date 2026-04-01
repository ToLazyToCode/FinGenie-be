package fingenie.com.fingenie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Main analytics response combining multiple analysis views.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {

    private PeriodSummary currentPeriod;
    private PeriodSummary previousPeriod;
    private List<CategoryBreakdown> categoryBreakdown;
    private List<DailyTrend> dailyTrend;
    private SpendingComparison comparison;
    private List<TopMerchant> topMerchants;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PeriodSummary {

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netAmount;
        private Integer transactionCount;
        private BigDecimal averageTransaction;
        private BigDecimal largestExpense;
        private String largestExpenseCategory;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryBreakdown {
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private BigDecimal amount;
        private Double percentage;
        private Integer transactionCount;
        private BigDecimal budget;
        private Double budgetUsedPercentage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyTrend {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal cumulative;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpendingComparison {
        private Double percentageChange;
        private BigDecimal absoluteChange;
        private String trend; // UP, DOWN, STABLE
        private String insight;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopMerchant {
        private String merchantName;
        private BigDecimal totalSpent;
        private Integer transactionCount;
        private String categoryName;
    }
}
