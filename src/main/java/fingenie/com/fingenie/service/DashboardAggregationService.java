package fingenie.com.fingenie.service;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.*;
import fingenie.com.fingenie.dto.DashboardResponse;
import fingenie.com.fingenie.dto.DashboardResponse.*;
import fingenie.com.fingenie.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardAggregationService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final StreakRepository streakRepository;
    private final StreakDailyLogRepository streakDailyLogRepository;
    private final FriendStreakRepository friendStreakRepository;
    private final UserGamificationRepository gamificationRepository;
    private final PetProfileRepository petProfileRepository;
    private final UserAISpendingProfileRepository aiProfileRepository;
    private final SpendingGuessRepository spendingGuessRepository;
    private final FinancialSummaryService financialSummaryService;

    private static final int XP_PER_LEVEL = 100;

    public DashboardResponse getDashboard() {
        Account account = SecurityUtils.getCurrentAccount();
        Long accountId = account.getId();

        return DashboardResponse.builder()
                .balance(buildBalanceSummary(account))
                .streak(buildStreakSummary(accountId))
                .xp(buildXpSummary(accountId))
                .pet(buildPetSummary(accountId))
                .aiInsight(buildAiInsight(accountId))
                .todayGuess(buildTodayGuess(accountId))
                .suggestedActions(buildSuggestedActions(accountId))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private BalanceSummary buildBalanceSummary(Account account) {
        List<Wallet> wallets = walletRepository.findByAccount(account);
        
        BigDecimal totalBalance = wallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get this month's income/expense
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        List<Transaction> monthTransactions = transactionRepository
                .findByAccountAndTransactionDateBetween(
                        account, 
                        java.sql.Date.valueOf(startOfMonth), 
                        java.sql.Date.valueOf(endOfMonth));

        BigDecimal totalIncome = monthTransactions.stream()
                .filter(t -> t.getCategory().getCategoryType() == Category.CategoryType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = monthTransactions.stream()
                .filter(t -> t.getCategory().getCategoryType() == Category.CategoryType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate month-over-month change (simplified)
        BigDecimal monthlyChange = BigDecimal.ZERO;
        if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            monthlyChange = totalIncome.subtract(totalExpense)
                    .divide(totalExpense, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return BalanceSummary.builder()
                .totalBalance(totalBalance)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .monthlyChange(monthlyChange)
                .currency("VND")
                .build();
    }

    private StreakSummary buildStreakSummary(Long accountId) {
        Optional<Streak> streakOpt = streakRepository.findByAccountId(accountId);
        
        int currentStreak = 0;
        int longestStreak = 0;
        boolean todayLogged = false;
        String status = "ACTIVE";

        if (streakOpt.isPresent()) {
            Streak streak = streakOpt.get();
            currentStreak = streak.getCurrentStreak();
            longestStreak = streak.getLongestStreak();
            
            // Check if today is logged
            todayLogged = streakDailyLogRepository
                    .findByAccountIdAndLogDate(accountId, LocalDate.now())
                    .isPresent();

            // Determine streak status
            if (!todayLogged && currentStreak > 0) {
                status = "AT_RISK";
            } else if (currentStreak == 0) {
                status = "BROKEN";
            }
        }

        // Count active friend streaks
        int friendStreakCount = friendStreakRepository.countByAccountIdAndIsActiveTrue(accountId);

        return StreakSummary.builder()
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .todayLogged(todayLogged)
                .friendStreakCount(friendStreakCount)
                .streakStatus(status)
                .build();
    }

    private XpSummary buildXpSummary(Long accountId) {
        UserGamification gamification = gamificationRepository.findByAccountId(accountId)
                .orElse(UserGamification.builder().accountId(accountId).xp(0).level(1).build());

        int currentXp = gamification.getXp();
        int level = gamification.getLevel();
        int xpForCurrentLevel = (level - 1) * XP_PER_LEVEL;
        int xpForNextLevel = level * XP_PER_LEVEL;
        int xpInCurrentLevel = currentXp - xpForCurrentLevel;
        int xpNeededForNext = XP_PER_LEVEL;
        double progress = (double) xpInCurrentLevel / xpNeededForNext * 100;

        // Simplified: XP earned today would need XPLog tracking
        int xpEarnedToday = 0;

        return XpSummary.builder()
                .currentXp(currentXp)
                .currentLevel(level)
                .xpToNextLevel(xpNeededForNext - xpInCurrentLevel)
                .totalXpForNextLevel(xpNeededForNext)
                .progressPercent(Math.min(progress, 100))
                .xpEarnedToday(xpEarnedToday)
                .build();
    }

    private PetSummary buildPetSummary(Long accountId) {
        PetProfile pet = petProfileRepository.findByAccountId(accountId)
                .orElse(PetProfile.builder()
                        .accountId(accountId)
                        .mood(50)
                        .happiness(50)
                        .energy(50)
                        .hunger(50)
                        .build());

        String moodState = determineMoodState(pet.getMood(), pet.getEnergy());
        String message = generatePetMessage(moodState, pet);

        return PetSummary.builder()
                .mood(pet.getMood())
                .happiness(pet.getHappiness())
                .energy(pet.getEnergy())
                .hunger(pet.getHunger())
                .moodState(moodState)
                .message(message)
                .build();
    }

    private String determineMoodState(int mood, int energy) {
        if (energy < 20) return "SLEEPING";
        if (mood >= 80) return "EXCITED";
        if (mood >= 60) return "HAPPY";
        if (mood >= 40) return "NEUTRAL";
        return "SAD";
    }

    private String generatePetMessage(String moodState, PetProfile pet) {
        return switch (moodState) {
            case "EXCITED" -> "Amazing! You're doing so well with your finances!";
            case "HAPPY" -> "Great job keeping track of your spending!";
            case "NEUTRAL" -> "Let's log some transactions today!";
            case "SAD" -> "I miss you... Let's get back on track!";
            case "SLEEPING" -> "Zzz... Wake me up with some activity!";
            default -> "Hello! Ready to manage your finances?";
        };
    }

    private AiInsightSummary buildAiInsight(Long accountId) {
        // Get user AI profile for personalized insight
        Optional<UserAISpendingProfile> profileOpt = aiProfileRepository.findByUserId(accountId);

        String fallbackInsightText = "Track your spending to get personalized insights!";
        String insightType = "TIP";
        double confidence = 0.5;
        String actionSuggestion = "Add your first transaction";
        BigDecimal baselineForAi = null;
        BigDecimal accuracyForAi = null;

        if (profileOpt.isPresent()) {
            UserAISpendingProfile profile = profileOpt.get();
            BigDecimal baseline = profile.getBaselineDailySpending();
            baselineForAi = baseline;
            accuracyForAi = profile.getPredictionAccuracyRate();
            
            if (baseline != null && baseline.compareTo(BigDecimal.ZERO) > 0) {
                confidence = profile.getConfidenceScore().doubleValue();
                
                if (profile.getPredictionAccuracyRate().compareTo(new BigDecimal("0.7")) > 0) {
                    fallbackInsightText = String.format("Your predictions are %d%% accurate! Keep validating AI suggestions.",
                            profile.getPredictionAccuracyRate().multiply(BigDecimal.valueOf(100)).intValue());
                    insightType = "CELEBRATION";
                    actionSuggestion = "Check today's AI guess";
                } else {
                    fallbackInsightText = String.format("Your typical daily spending is around %,.0f VND",
                            baseline.doubleValue());
                    insightType = "TIP";
                    actionSuggestion = "Review your spending patterns";
                }
            }
        }
        String insightText = financialSummaryService.buildHomeSummary(
                accountId,
                baselineForAi,
                accuracyForAi,
                fallbackInsightText
        );

        return AiInsightSummary.builder()
                .insightText(insightText)
                .insightType(insightType)
                .confidence(confidence)
                .actionSuggestion(actionSuggestion)
                .build();
    }

    private SpendingGuessSummary buildTodayGuess(Long accountId) {
        // Find active (pending) guess for today
        Optional<SpendingGuess> guessOpt = spendingGuessRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(accountId, "PENDING");

        if (guessOpt.isEmpty()) {
            return SpendingGuessSummary.builder()
                    .hasActiveGuess(false)
                    .build();
        }

        SpendingGuess guess = guessOpt.get();
        return SpendingGuessSummary.builder()
                .guessId(guess.getId())
                .amount(guess.getGuessedAmount())
                .category(guess.getCategory() != null ? guess.getCategory().getCategoryName() : "General")
                .walletName(guess.getWallet() != null ? guess.getWallet().getWalletName() : "Default")
                .confidence(guess.getConfidence().doubleValue())
                .reasoning(guess.getReasoning())
                .hasActiveGuess(true)
                .build();
    }

    private List<QuickAction> buildSuggestedActions(Long accountId) {
        List<QuickAction> actions = new ArrayList<>();

        // Check streak status
        Optional<Streak> streakOpt = streakRepository.findByAccountId(accountId);
        boolean todayLogged = streakDailyLogRepository
                .findByAccountIdAndLogDate(accountId, LocalDate.now())
                .isPresent();

        if (!todayLogged) {
            actions.add(QuickAction.builder()
                    .actionType("ADD_TRANSACTION")
                    .label("Log Today's Spending")
                    .description("Keep your streak alive!")
                    .priority(1)
                    .build());
        }

        // Check pet hunger
        PetProfile pet = petProfileRepository.findByAccountId(accountId).orElse(null);
        if (pet != null && pet.getHunger() > 70) {
            actions.add(QuickAction.builder()
                    .actionType("FEED_PET")
                    .label("Feed Your Pet")
                    .description("Your pet is getting hungry")
                    .priority(2)
                    .build());
        }

        // Check pending guess
        Optional<SpendingGuess> guessOpt = spendingGuessRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(accountId, "PENDING");
        if (guessOpt.isPresent()) {
            actions.add(QuickAction.builder()
                    .actionType("REVIEW_GUESS")
                    .label("AI Spending Prediction")
                    .description("Review and confirm AI's guess")
                    .priority(1)
                    .build());
        }

        // Default action if nothing else
        if (actions.isEmpty()) {
            actions.add(QuickAction.builder()
                    .actionType("VIEW_ANALYTICS")
                    .label("View Spending Analysis")
                    .description("Check your financial trends")
                    .priority(3)
                    .build());
        }

        return actions;
    }
}
