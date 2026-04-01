package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.Budget.PeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {
    
    private Long id;
    private Long accountId;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private int usagePercent;
    private PeriodType periodType;
    private Integer alertThreshold;
    private Boolean isActive;
    private Boolean notifyOnExceed;
    private Boolean notifyOnWarning;
    private Boolean rolloverExcess;
    private String notes;
    private BudgetStatus status;
    private String statusMessage;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    public enum BudgetStatus {
        UNDER_BUDGET,
        WARNING,
        NEAR_LIMIT,
        EXCEEDED,
        INACTIVE
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetSummary {
        private BigDecimal totalBudget;
        private BigDecimal totalSpent;
        private BigDecimal totalRemaining;
        private int overallUsagePercent;
        private int budgetsUnderControl;
        private int budgetsAtWarning;
        private int budgetsExceeded;
        private List<BudgetResponse> categoryBudgets;
        private BudgetResponse totalBudgetInfo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetAlert {
        private Long budgetId;
        private Long categoryId;
        private String categoryName;
        private BudgetStatus status;
        private int usagePercent;
        private BigDecimal overspentBy;
        private String message;
        private Timestamp timestamp;
    }
}
