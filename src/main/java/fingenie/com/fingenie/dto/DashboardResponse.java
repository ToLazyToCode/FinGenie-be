package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private BalanceSummary balance;
    private StreakSummary streak;
    private XpSummary xp;
    private PetSummary pet;
    private AiInsightSummary aiInsight;
    private SpendingGuessSummary todayGuess;
    private List<QuickAction> suggestedActions;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceSummary {
        private BigDecimal totalBalance;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal monthlyChange; // % change from last month
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakSummary {
        private int currentStreak;
        private int longestStreak;
        private boolean todayLogged;
        private int friendStreakCount; // active friend streaks
        private String streakStatus; // ACTIVE, AT_RISK, BROKEN
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class XpSummary {
        private int currentXp;
        private int currentLevel;
        private int xpToNextLevel;
        private int totalXpForNextLevel;
        private double progressPercent;
        private int xpEarnedToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetSummary {
        private int mood; // 0-100
        private int happiness; // 0-100
        private int energy; // 0-100
        private int hunger; // 0-100
        private String moodState; // HAPPY, SAD, NEUTRAL, EXCITED, SLEEPING
        private String message; // AI-generated pet message
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiInsightSummary {
        private String insightText;
        private String insightType; // TIP, WARNING, CELEBRATION, SUGGESTION
        private double confidence;
        private String actionSuggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpendingGuessSummary {
        private Long guessId;
        private BigDecimal amount;
        private String category;
        private String walletName;
        private double confidence;
        private String reasoning;
        private boolean hasActiveGuess;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickAction {
        private String actionType; // ADD_TRANSACTION, CHECK_BUDGET, FEED_PET, VIEW_GOAL
        private String label;
        private String description;
        private int priority;
    }
}
