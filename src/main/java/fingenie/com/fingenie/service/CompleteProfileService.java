package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.dto.CompleteProfileResponse.*;
import fingenie.com.fingenie.entity.*;
import fingenie.com.fingenie.entity.Achievement;
import fingenie.com.fingenie.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteProfileService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLevelRepository userLevelRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PiggyGoalRepository piggyGoalRepository;
    private final BudgetRepository budgetRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final fingenie.com.fingenie.repository.AchievementRepository achievementRepository;
    
    @Transactional(readOnly = true)
    public CompleteProfileResponse getCompleteProfile(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        UserProfile profile = userProfileRepository.findByAccountId(accountId).orElse(null);
        UserLevel userLevel = userLevelRepository.findByAccountId(accountId).orElse(null);
        
        return CompleteProfileResponse.builder()
                .userId(profile != null ? profile.getId() : null)
                .accountId(accountId)
                .email(account.getEmail())
                .fullName(profile != null ? profile.getFullName() : account.getName())
                .avatarUrl(account.getAvatarUrl())
                .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
                .memberSince(account.getCreatedAt())
                .gamification(buildGamificationStats(accountId, userLevel))
                .financial(buildFinancialSummary(accountId))
                .activity(buildActivityStats(accountId))
                .recentAchievements(getRecentAchievements(accountId))
                .build();
    }
    
    @Transactional
    public UserSettingsResponse getSettings(Long accountId) {
        UserSettings settings = userSettingsRepository.findByAccountId(accountId)
                .orElseGet(() -> createDefaultSettings(accountId));
        
        return toSettingsResponse(settings);
    }
    
    @Transactional
    public UserSettingsResponse updateSettings(Long accountId, UserSettingsRequest request) {
        UserSettings settings = userSettingsRepository.findByAccountId(accountId)
                .orElseGet(() -> createDefaultSettings(accountId));
        
        if (request.getCurrency() != null) settings.setCurrency(request.getCurrency());
        if (request.getLocale() != null) settings.setLocale(request.getLocale());
        if (request.getTheme() != null) settings.setTheme(request.getTheme());
        if (request.getDateFormat() != null) settings.setDateFormat(request.getDateFormat());
        if (request.getShowProfilePublicly() != null) settings.setShowProfilePublicly(request.getShowProfilePublicly());
        if (request.getShowInLeaderboard() != null) settings.setShowInLeaderboard(request.getShowInLeaderboard());
        if (request.getAllowFriendRequests() != null) settings.setAllowFriendRequests(request.getAllowFriendRequests());
        if (request.getShowLastActive() != null) settings.setShowLastActive(request.getShowLastActive());
        if (request.getAllowAILearning() != null) settings.setAllowAILearning(request.getAllowAILearning());
        if (request.getProactiveGuessPrompts() != null) settings.setProactiveGuessPrompts(request.getProactiveGuessPrompts());
        if (request.getPreferredAIPersonality() != null) settings.setPreferredAIPersonality(request.getPreferredAIPersonality());
        if (request.getRequireBiometricForTransactions() != null) 
            settings.setRequireBiometricForTransactions(request.getRequireBiometricForTransactions());
        if (request.getSessionTimeout() != null) settings.setSessionTimeoutEnabled(request.getSessionTimeout());
        if (request.getSessionTimeoutMinutes() != null) settings.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
        
        UserSettings saved = userSettingsRepository.save(settings);
        return toSettingsResponse(saved);
    }
    
    private GamificationStats buildGamificationStats(Long accountId, UserLevel userLevel) {
        int level = userLevel != null ? userLevel.getCurrentLevel() : 1;
        int currentXp = userLevel != null ? userLevel.getCurrentXp() : 0;
        int lifetimeXp = userLevel != null ? userLevel.getLifetimeXp() : 0;
        
        // XP required for next level (simple formula: level * 100)
        int xpForNextLevel = level * 100;
        int xpProgress = xpForNextLevel > 0 ? (currentXp * 100 / xpForNextLevel) : 0;
        
        Long globalRank = userLevelRepository.findRankByAccountId(accountId);
        
        int achievementsUnlocked = (int) userAchievementRepository.countByAccountId(accountId);
        
        return GamificationStats.builder()
                .level(level)
                .currentXp(currentXp)
                .lifetimeXp(lifetimeXp)
                .xpToNextLevel(xpForNextLevel - currentXp)
                .xpProgress(xpProgress)
                .currentStreak(0) // Would need streak repository
                .longestStreak(0) // Would need streak repository
                .achievementsUnlocked(achievementsUnlocked)
                .totalAchievements(20) // Could query Achievement count
                .globalRank(globalRank)
                .badge(getLevelBadge(level))
                .title(getLevelTitle(level))
                .build();
    }
    
    private FinancialSummary buildFinancialSummary(Long accountId) {
        List<Wallet> wallets = walletRepository.findByAccountId(accountId);
        BigDecimal totalBalance = wallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        LocalDate today = LocalDate.now();
        Date startOfMonth = Date.valueOf(today.withDayOfMonth(1));
        Date endOfMonth = Date.valueOf(today.with(TemporalAdjusters.lastDayOfMonth()));
        
        BigDecimal monthlyIncome = transactionRepository.sumIncomeByAccountIdAndDateRange(
                accountId, startOfMonth, endOfMonth);
        BigDecimal monthlyExpenses = transactionRepository.sumExpenseByAccountIdAndDateRange(
                accountId, startOfMonth, endOfMonth);
        
        int savingsGoalsActive = piggyGoalRepository.findActiveGoals(accountId).size();
        BigDecimal totalSaved = piggyGoalRepository.getTotalSaved(accountId);
        
        List<Budget> budgets = budgetRepository.findByAccountIdAndIsActiveTrue(accountId);
        int budgetsOnTrack = 0; // Would need to calculate from usage
        
        long transactionCount = transactionRepository.countByAccountIdAndDateRange(
                accountId, startOfMonth, endOfMonth);
        
        return FinancialSummary.builder()
                .totalWallets(wallets.size())
                .totalBalance(totalBalance)
                .totalTransactions((int) transactionCount)
                .monthlyIncome(monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                .monthlyExpenses(monthlyExpenses != null ? monthlyExpenses : BigDecimal.ZERO)
                .savingsGoalsActive(savingsGoalsActive)
                .totalSaved(totalSaved != null ? totalSaved : BigDecimal.ZERO)
                .budgetsActive(budgets.size())
                .budgetsOnTrack(budgetsOnTrack)
                .build();
    }
    
    private ActivityStats buildActivityStats(Long accountId) {
        long friendsCount = friendshipRepository.countFriends(accountId);
        
        return ActivityStats.builder()
                .daysActive(0) // Would need activity tracking
                .transactionsThisMonth(0) // Calculated in financial summary
                .guessesSubmitted(0) // Would need guess repository
                .guessAccuracy(0) // Would need calculation
                .friendsCount((int) friendsCount)
                .build();
    }
    
    private List<AchievementInfo> getRecentAchievements(Long accountId) {
        return userAchievementRepository.findRecentByAccountId(accountId, PageRequest.of(0, 5))
                .stream()
                .map(ua -> {
                    Achievement achievement = achievementRepository.findById(ua.getAchievementId()).orElse(null);
                    if (achievement == null) {
                        return null;
                    }
                    return AchievementInfo.builder()
                            .id(achievement.getId())
                            .code(achievement.getCode())
                            .name(achievement.getName())
                            .tier(achievement.getTier().name())
                            .xpReward(achievement.getXpReward())
                            .unlockedAt(ua.getCreatedAt())
                            .build();
                })
                .filter(a -> a != null)
                .collect(Collectors.toList());
    }
    
    private UserSettings createDefaultSettings(Long accountId) {
        UserSettings settings = UserSettings.builder()
                .accountId(accountId)
                .build();
        return userSettingsRepository.save(settings);
    }
    
    private UserSettingsResponse toSettingsResponse(UserSettings settings) {
        return UserSettingsResponse.builder()
                .currency(settings.getCurrency())
                .locale(settings.getLocale())
                .theme(settings.getTheme())
                .dateFormat(settings.getDateFormat())
                .showProfilePublicly(settings.getShowProfilePublicly())
                .showInLeaderboard(settings.getShowInLeaderboard())
                .allowFriendRequests(settings.getAllowFriendRequests())
                .showLastActive(settings.getShowLastActive())
                .allowAILearning(settings.getAllowAILearning())
                .proactiveGuessPrompts(settings.getProactiveGuessPrompts())
                .preferredAIPersonality(settings.getPreferredAIPersonality())
                .requireBiometricForTransactions(settings.getRequireBiometricForTransactions())
                .sessionTimeoutEnabled(settings.getSessionTimeoutEnabled())
                .sessionTimeoutMinutes(settings.getSessionTimeoutMinutes())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
    
    private String getLevelBadge(int level) {
        if (level >= 50) return "LEGEND";
        if (level >= 40) return "MASTER";
        if (level >= 30) return "EXPERT";
        if (level >= 20) return "ADVANCED";
        if (level >= 10) return "INTERMEDIATE";
        if (level >= 5) return "BEGINNER";
        return "ROOKIE";
    }
    
    private String getLevelTitle(int level) {
        if (level >= 50) return "Financial Legend";
        if (level >= 40) return "Money Master";
        if (level >= 30) return "Savings Expert";
        if (level >= 20) return "Budget Pro";
        if (level >= 10) return "Finance Tracker";
        if (level >= 5) return "Smart Saver";
        return "Finance Rookie";
    }
}
