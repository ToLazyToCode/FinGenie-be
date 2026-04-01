package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * Complete user profile response with all stats and gamification data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteProfileResponse {
    
    // Basic Info
    private Long userId;
    private Long accountId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Date dateOfBirth;
    private Timestamp memberSince;
    
    // Gamification Stats
    private GamificationStats gamification;
    
    // Financial Summary
    private FinancialSummary financial;
    
    // Activity Stats
    private ActivityStats activity;
    
    // Pet Info (if has pet)
    private PetSummary pet;
    
    // Recent Achievements
    private List<AchievementInfo> recentAchievements;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GamificationStats {
        private int level;
        private int currentXp;
        private int lifetimeXp;
        private int xpToNextLevel;
        private int xpProgress; // percentage
        private int currentStreak;
        private int longestStreak;
        private int achievementsUnlocked;
        private int totalAchievements;
        private Long globalRank;
        private Long friendsRank;
        private String badge;
        private String title;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialSummary {
        private int totalWallets;
        private BigDecimal totalBalance;
        private int totalTransactions;
        private BigDecimal monthlyIncome;
        private BigDecimal monthlyExpenses;
        private int savingsGoalsActive;
        private BigDecimal totalSaved;
        private int budgetsActive;
        private int budgetsOnTrack;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityStats {
        private int daysActive;
        private int transactionsThisMonth;
        private int guessesSubmitted;
        private int guessAccuracy; // percentage
        private Timestamp lastActive;
        private int friendsCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetSummary {
        private Long petId;
        private String petName;
        private String petType;
        private int petLevel;
        private String mood;
        private int happinessScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementInfo {
        private Long id;
        private String code;
        private String name;
        private String tier;
        private int xpReward;
        private Timestamp unlockedAt;
    }
}
