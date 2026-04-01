package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PiggyGoalResponse {
    
    private Long id;
    private Long accountId;
    private String title;
    private String iconUrl;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private int progressPercent;
    private Instant deadline;
    private long daysRemaining;
    private boolean isCompleted;
    private boolean isOverdue;
    private BigDecimal suggestedDailyAmount;
    private Instant createdAt;
    private Instant updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PiggyGoalSummary {
        private int totalGoals;
        private int completedGoals;
        private int activeGoals;
        private BigDecimal totalSaved;
        private BigDecimal totalTargeted;
        private int overallProgressPercent;
        private List<PiggyGoalResponse> goals;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositResult {
        private PiggyGoalResponse goal;
        private BigDecimal depositedAmount;
        private boolean goalJustCompleted;
        private int xpAwarded;
        private String achievementUnlocked;
    }
}
