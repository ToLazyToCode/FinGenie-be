package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.UserAchievement;
import fingenie.com.fingenie.dto.AchievementResponse;
import fingenie.com.fingenie.entity.Achievement;
import fingenie.com.fingenie.entity.Achievement.AchievementCategory;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.Streak;
import fingenie.com.fingenie.entity.UserGamification;
import fingenie.com.fingenie.repository.AchievementRepository;
import fingenie.com.fingenie.repository.PiggyGoalRepository;
import fingenie.com.fingenie.repository.SavingContributionRepository;
import fingenie.com.fingenie.repository.StreakRepository;
import fingenie.com.fingenie.repository.TransactionRepository;
import fingenie.com.fingenie.repository.UserGamificationRepository;
import fingenie.com.fingenie.repository.UserSubscriptionRepository;
import fingenie.com.fingenie.repository.UserAchievementRepository;
import fingenie.com.fingenie.survey.repository.UserBehaviorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;
    private final TransactionRepository transactionRepository;
    private final PiggyGoalRepository piggyGoalRepository;
    private final SavingContributionRepository savingContributionRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserBehaviorProfileRepository userBehaviorProfileRepository;
    private final StreakRepository streakRepository;

    /**
     * Get all achievements with user progress.
     */
    @Transactional
    public List<AchievementResponse> getAllAchievementsForUser(Long accountId) {
        backfillLegacyAchievements(accountId);
        List<Achievement> achievements = achievementRepository.findAllActiveSorted();
        Map<Long, UserAchievement> userProgress = userAchievementRepository.findByAccountId(accountId)
                .stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, Function.identity()));

        return achievements.stream()
                .filter(a -> !a.getIsHidden() || userProgress.containsKey(a.getId()))
                .map(a -> toResponse(a, userProgress.get(a.getId())))
                .toList();
    }

    /**
     * Get achievements by category.
     */
    @Transactional
    public List<AchievementResponse> getAchievementsByCategory(Long accountId, AchievementCategory category) {
        backfillLegacyAchievements(accountId);
        List<Achievement> achievements = achievementRepository.findByCategoryAndIsActiveTrue(category);
        Map<Long, UserAchievement> userProgress = userAchievementRepository.findByAccountId(accountId)
                .stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, Function.identity()));

        return achievements.stream()
                .map(a -> toResponse(a, userProgress.get(a.getId())))
                .toList();
    }

    /**
     * Get claimable achievements.
     */
    @Transactional
    public List<AchievementResponse> getClaimableAchievements(Long accountId) {
        backfillLegacyAchievements(accountId);
        List<UserAchievement> claimable = userAchievementRepository.findByAccountIdAndIsUnlockedTrueAndIsClaimedFalse(accountId);
        
        return claimable.stream()
                .map(ua -> {
                    Achievement achievement = achievementRepository.findById(ua.getAchievementId()).orElse(null);
                    return achievement != null ? toResponse(achievement, ua) : null;
                })
                .filter(r -> r != null)
                .toList();
    }

    /**
     * Claim an achievement reward.
     */
    @Transactional
    public AchievementResponse claimAchievement(Long accountId, Long achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found"));

        UserAchievement userAchievement = userAchievementRepository.findByAccountIdAndAchievementId(accountId, achievementId)
                .orElseThrow(() -> new IllegalArgumentException("Achievement progress not found"));

        if (!userAchievement.getIsUnlocked()) {
            throw new IllegalStateException("Achievement not yet unlocked");
        }

        if (userAchievement.getIsClaimed()) {
            throw new IllegalStateException("Achievement already claimed");
        }

        // Award XP
        gamificationService.addXp(accountId, achievement.getXpReward());

        // Mark as claimed
        userAchievement.setIsClaimed(true);
        userAchievement.setClaimedAt(LocalDateTime.now());
        userAchievementRepository.save(userAchievement);

        log.info("User {} claimed achievement {}", accountId, achievement.getCode());
        return toResponse(achievement, userAchievement);
    }

    /**
     * Update progress for an achievement.
     */
    @Transactional
    public void updateProgress(Long accountId, String achievementCode, int incrementBy) {
        Achievement achievement = achievementRepository.findByCode(achievementCode)
                .orElse(null);

        if (achievement == null || !achievement.getIsActive()) {
            return;
        }

        UserAchievement userAchievement = userAchievementRepository
                .findByAccountIdAndAchievementId(accountId, achievement.getId())
                .orElseGet(() -> {
                    UserAchievement newProgress = UserAchievement.builder()
                            .accountId(accountId)
                            .achievementId(achievement.getId())
                            .progressValue(0)
                            .isUnlocked(false)
                            .isClaimed(false)
                            .build();
                    return userAchievementRepository.save(newProgress);
                });

        if (userAchievement.getIsUnlocked()) {
            return; // Already unlocked
        }

        userAchievement.setProgressValue(userAchievement.getProgressValue() + incrementBy);

        // Check if unlocked
        if (userAchievement.getProgressValue() >= achievement.getTargetValue()) {
            userAchievement.setIsUnlocked(true);
            userAchievement.setUnlockedAt(LocalDateTime.now());
            log.info("User {} unlocked achievement {}", accountId, achievement.getCode());

            // Send notification
            notificationService.notifyAchievementUnlocked(accountId, achievement.getName(), achievement.getXpReward());
        }

        userAchievementRepository.save(userAchievement);
    }

    /**
     * Set progress to a specific value (for metrics-based achievements).
     */
    @Transactional
    public void setProgress(Long accountId, String achievementCode, int value) {
        Achievement achievement = achievementRepository.findByCode(achievementCode)
                .orElse(null);

        if (achievement == null || !achievement.getIsActive()) {
            return;
        }

        UserAchievement userAchievement = userAchievementRepository
                .findByAccountIdAndAchievementId(accountId, achievement.getId())
                .orElseGet(() -> {
                    UserAchievement newProgress = UserAchievement.builder()
                            .accountId(accountId)
                            .achievementId(achievement.getId())
                            .progressValue(0)
                            .isUnlocked(false)
                            .isClaimed(false)
                            .build();
                    return userAchievementRepository.save(newProgress);
                });

        if (userAchievement.getIsUnlocked()) {
            return;
        }

        userAchievement.setProgressValue(value);

        if (userAchievement.getProgressValue() >= achievement.getTargetValue()) {
            userAchievement.setIsUnlocked(true);
            userAchievement.setUnlockedAt(LocalDateTime.now());
            notificationService.notifyAchievementUnlocked(accountId, achievement.getName(), achievement.getXpReward());
        }

        userAchievementRepository.save(userAchievement);
    }

    /**
     * Get achievement summary for dashboard.
     */
    @Transactional(readOnly = true)
    public AchievementSummary getSummary(Long accountId) {
        backfillLegacyAchievements(accountId);
        long totalAchievements = achievementRepository.count();
        long unlockedCount = userAchievementRepository.countUnlockedByAccountId(accountId);
        long claimableCount = userAchievementRepository.countClaimableByAccountId(accountId);

        return AchievementSummary.builder()
                .totalAchievements((int) totalAchievements)
                .unlockedCount((int) unlockedCount)
                .claimableCount((int) claimableCount)
                .completionPercentage(totalAchievements > 0 ? (unlockedCount * 100.0 / totalAchievements) : 0)
                .build();
    }

    // ==================== Private Helpers ====================

    private void backfillLegacyAchievements(Long accountId) {
        List<Achievement> definitions = achievementRepository.findAllActiveSorted();
        if (definitions.isEmpty()) {
            return;
        }

        Map<String, Achievement> byCode = definitions.stream()
                .collect(Collectors.toMap(Achievement::getCode, Function.identity(), (left, ignored) -> left, HashMap::new));
        Map<Long, UserAchievement> progressByAchievementId = userAchievementRepository.findByAccountId(accountId)
                .stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, Function.identity(), (left, ignored) -> left, HashMap::new));

        long totalTransactions = transactionRepository.countTotalByAccountId(accountId);
        long incomeTransactions = transactionRepository.countByAccountIdAndCategoryType(accountId, Category.CategoryType.INCOME);
        long expenseTransactions = transactionRepository.countByAccountIdAndCategoryType(accountId, Category.CategoryType.EXPENSE);
        long totalGoals = piggyGoalRepository.findByAccountId(accountId).size();
        BigDecimal savedFromContributions = savingContributionRepository.sumAmountByAccountId(accountId);
        long savedAmount = savedFromContributions == null ? 0L : savedFromContributions.longValue();
        int userLevel = userGamificationRepository.findByAccountId(accountId)
                .map(UserGamification::getLevel)
                .orElse(1);
        int maxStreak = streakRepository.findByAccountId(accountId)
                .map(streak -> Math.max(streak.getCurrentStreak(), streak.getLongestStreak()))
                .orElse(0);
        boolean hasSurveyProfile = userBehaviorProfileRepository.findByUserId(accountId).isPresent();
        boolean hasSubscription = userSubscriptionRepository.findByAccountId(accountId).isPresent();

        unlockIfConditionMet(accountId, byCode.get("SURVEY_FIRST_COMPLETE"), hasSurveyProfile, 1, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("FIRST_TRANSACTION"), totalTransactions >= 1, 1, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("FIRST_INCOME"), incomeTransactions >= 1, 1, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("FIRST_EXPENSE"), expenseTransactions >= 1, 1, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("STREAK_7_DAYS"), maxStreak >= 7, maxStreak, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("SAVE_100K"), savedAmount >= 100_000L, savedAmount, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("SAVE_1M"), savedAmount >= 1_000_000L, savedAmount, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("FIRST_GOAL"), totalGoals >= 1, totalGoals, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("PET_LEVEL_3"), userLevel >= 3, userLevel, progressByAchievementId);
        unlockIfConditionMet(accountId, byCode.get("FIRST_SUBSCRIPTION_UPGRADE"), hasSubscription, 1, progressByAchievementId);
    }

    private void unlockIfConditionMet(
            Long accountId,
            Achievement achievement,
            boolean unlocked,
            long progressValue,
            Map<Long, UserAchievement> progressByAchievementId
    ) {
        if (achievement == null) {
            return;
        }

        UserAchievement existing = progressByAchievementId.get(achievement.getId());
        int target = achievement.getTargetValue() == null ? 1 : achievement.getTargetValue();
        int normalizedProgress = (int) Math.min(Integer.MAX_VALUE, Math.max(progressValue, 0L));

        if (existing == null) {
            if (!unlocked && normalizedProgress <= 0) {
                return;
            }
            UserAchievement created = UserAchievement.builder()
                    .accountId(accountId)
                    .achievementId(achievement.getId())
                    .progressValue(Math.max(normalizedProgress, unlocked ? target : 0))
                    .isUnlocked(unlocked)
                    .unlockedAt(unlocked ? LocalDateTime.now() : null)
                    .isClaimed(false)
                    .build();
            UserAchievement saved = userAchievementRepository.save(created);
            progressByAchievementId.put(saved.getAchievementId(), saved);
            return;
        }

        boolean changed = false;
        if (normalizedProgress > (existing.getProgressValue() == null ? 0 : existing.getProgressValue())) {
            existing.setProgressValue(normalizedProgress);
            changed = true;
        }
        if (unlocked && !Boolean.TRUE.equals(existing.getIsUnlocked())) {
            existing.setIsUnlocked(true);
            existing.setUnlockedAt(LocalDateTime.now());
            if ((existing.getProgressValue() == null ? 0 : existing.getProgressValue()) < target) {
                existing.setProgressValue(target);
            }
            changed = true;
        }

        if (changed) {
            userAchievementRepository.save(existing);
        }
    }

    private AchievementResponse toResponse(Achievement achievement, UserAchievement userProgress) {
        int progress = userProgress != null ? userProgress.getProgressValue() : 0;
        double progressPct = (progress * 100.0) / achievement.getTargetValue();

        return AchievementResponse.builder()
                .id(achievement.getId())
                .code(achievement.getCode())
                .name(achievement.getName())
                .description(achievement.getDescription())
                .icon(achievement.getIcon())
                .tier(achievement.getTier().name())
                .category(achievement.getCategory().name())
                .xpReward(achievement.getXpReward())
                .targetValue(achievement.getTargetValue())
                .isHidden(achievement.getIsHidden())
                .progressValue(progress)
                .progressPercentage(Math.min(progressPct, 100))
                .isUnlocked(userProgress != null && userProgress.getIsUnlocked())
                .isClaimed(userProgress != null && userProgress.getIsClaimed())
                .unlockedAt(userProgress != null ? userProgress.getUnlockedAt() : null)
                .claimedAt(userProgress != null ? userProgress.getClaimedAt() : null)
                .build();
    }

    // ==================== Achievement Definitions Seeder ====================

    // Runtime seed disabled: achievement definitions are seeded via SQL migration.
    public void seedAchievements() {
        if (achievementRepository.count() > 0) {
            return; // Already seeded
        }

        log.info("Seeding achievement definitions...");

        List<Achievement> achievements = List.of(
            // Transaction Achievements
            Achievement.builder()
                    .code("FIRST_TRANSACTION")
                    .name("Giao dịch đầu tiên")
                    .description("Ghi nhận giao dịch đầu tiên")
                    .icon("🚀")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.TRANSACTIONS)
                    .xpReward(10)
                    .targetValue(1)
                    .sortOrder(1)
                    .build(),
            Achievement.builder()
                    .code("TRANSACTION_10")
                    .name("Bắt đầu đều đặn")
                    .description("Ghi nhận 10 giao dịch")
                    .icon("📝")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.TRANSACTIONS)
                    .xpReward(25)
                    .targetValue(10)
                    .sortOrder(2)
                    .build(),
            Achievement.builder()
                    .code("TRANSACTION_50")
                    .name("Theo dõi giao dịch")
                    .description("Ghi nhận 50 giao dịch")
                    .icon("📊")
                    .tier(Achievement.AchievementTier.SILVER)
                    .category(AchievementCategory.TRANSACTIONS)
                    .xpReward(50)
                    .targetValue(50)
                    .sortOrder(3)
                    .build(),
            Achievement.builder()
                    .code("TRANSACTION_100")
                    .name("Bậc thầy tài chính")
                    .description("Ghi nhận 100 giao dịch")
                    .icon("🏆")
                    .tier(Achievement.AchievementTier.GOLD)
                    .category(AchievementCategory.TRANSACTIONS)
                    .xpReward(100)
                    .targetValue(100)
                    .sortOrder(4)
                    .build(),

            // Streak Achievements
            Achievement.builder()
                    .code("STREAK_7")
                    .name("Chiến binh 7 ngày")
                    .description("Duy trì chuỗi 7 ngày liên tiếp")
                    .icon("🔥")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.STREAKS)
                    .xpReward(30)
                    .targetValue(7)
                    .sortOrder(10)
                    .build(),
            Achievement.builder()
                    .code("STREAK_30")
                    .name("Bền bỉ 30 ngày")
                    .description("Duy trì chuỗi 30 ngày liên tiếp")
                    .icon("💪")
                    .tier(Achievement.AchievementTier.SILVER)
                    .category(AchievementCategory.STREAKS)
                    .xpReward(75)
                    .targetValue(30)
                    .sortOrder(11)
                    .build(),
            Achievement.builder()
                    .code("STREAK_100")
                    .name("Huyền thoại chuỗi")
                    .description("Duy trì chuỗi 100 ngày liên tiếp")
                    .icon("👑")
                    .tier(Achievement.AchievementTier.PLATINUM)
                    .category(AchievementCategory.STREAKS)
                    .xpReward(200)
                    .targetValue(100)
                    .sortOrder(12)
                    .build(),

            // AI Guess Achievements
            Achievement.builder()
                    .code("FIRST_GUESS")
                    .name("Dự đoán đầu tiên")
                    .description("Thực hiện dự đoán chi tiêu đầu tiên")
                    .icon("🔮")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.AI_GUESS)
                    .xpReward(15)
                    .targetValue(1)
                    .sortOrder(20)
                    .build(),
            Achievement.builder()
                    .code("ACCURATE_GUESS_5")
                    .name("Dự đoán chuẩn")
                    .description("Có 5 dự đoán với sai số trong 10%")
                    .icon("🎯")
                    .tier(Achievement.AchievementTier.SILVER)
                    .category(AchievementCategory.AI_GUESS)
                    .xpReward(50)
                    .targetValue(5)
                    .sortOrder(21)
                    .build(),
            Achievement.builder()
                    .code("GUESS_STREAK_7")
                    .name("Chuỗi dự đoán")
                    .description("Dự đoán liên tiếp 7 ngày")
                    .icon("📈")
                    .tier(Achievement.AchievementTier.GOLD)
                    .category(AchievementCategory.AI_GUESS)
                    .xpReward(75)
                    .targetValue(7)
                    .sortOrder(22)
                    .build(),

            // Savings Achievements
            Achievement.builder()
                    .code("FIRST_PIGGY")
                    .name("Mục tiêu đầu tiên")
                    .description("Tạo mục tiêu tiết kiệm đầu tiên")
                    .icon("🐷")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.SAVINGS)
                    .xpReward(20)
                    .targetValue(1)
                    .sortOrder(30)
                    .build(),
            Achievement.builder()
                    .code("GOAL_COMPLETE")
                    .name("Chinh phục mục tiêu")
                    .description("Hoàn thành một mục tiêu tiết kiệm")
                    .icon("🎉")
                    .tier(Achievement.AchievementTier.GOLD)
                    .category(AchievementCategory.SAVINGS)
                    .xpReward(100)
                    .targetValue(1)
                    .sortOrder(31)
                    .build(),

            // Social Achievements
            Achievement.builder()
                    .code("FIRST_FRIEND")
                    .name("Kết bạn đầu tiên")
                    .description("Thêm người bạn đầu tiên")
                    .icon("🤝")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.SOCIAL)
                    .xpReward(15)
                    .targetValue(1)
                    .sortOrder(40)
                    .build(),
            Achievement.builder()
                    .code("FRIENDS_5")
                    .name("Vòng tròn mở rộng")
                    .description("Có 5 người bạn")
                    .icon("👥")
                    .tier(Achievement.AchievementTier.SILVER)
                    .category(AchievementCategory.SOCIAL)
                    .xpReward(40)
                    .targetValue(5)
                    .sortOrder(41)
                    .build(),

            // Pet Achievements
            Achievement.builder()
                    .code("PET_HAPPY")
                    .name("Bạn thân của pet")
                    .description("Giữ pet vui vẻ trong 7 ngày")
                    .icon("😊")
                    .tier(Achievement.AchievementTier.SILVER)
                    .category(AchievementCategory.PET)
                    .xpReward(40)
                    .targetValue(7)
                    .sortOrder(50)
                    .build(),
            Achievement.builder()
                    .code("PET_CHAT_10")
                    .name("Tám cùng pet")
                    .description("Trò chuyện với pet 10 lần")
                    .icon("💬")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.PET)
                    .xpReward(25)
                    .targetValue(10)
                    .sortOrder(51)
                    .build(),

            // Milestones
            Achievement.builder()
                    .code("LEVEL_5")
                    .name("Vươn lên cấp 5")
                    .description("Đạt cấp độ 5")
                    .icon("⭐")
                    .tier(Achievement.AchievementTier.BRONZE)
                    .category(AchievementCategory.MILESTONES)
                    .xpReward(50)
                    .targetValue(5)
                    .sortOrder(60)
                    .build(),
            Achievement.builder()
                    .code("LEVEL_10")
                    .name("Anh hùng tài chính")
                    .description("Đạt cấp độ 10")
                    .icon("🌟")
                    .tier(Achievement.AchievementTier.SILVER)
                    .category(AchievementCategory.MILESTONES)
                    .xpReward(100)
                    .targetValue(10)
                    .sortOrder(61)
                    .build(),
            Achievement.builder()
                    .code("LEVEL_25")
                    .name("Huyền thoại tài chính")
                    .description("Đạt cấp độ 25")
                    .icon("💎")
                    .tier(Achievement.AchievementTier.PLATINUM)
                    .category(AchievementCategory.MILESTONES)
                    .xpReward(250)
                    .targetValue(25)
                    .sortOrder(62)
                    .build()
        );

        achievementRepository.saveAll(achievements);
        log.info("Seeded {} achievements", achievements.size());
    }

    // ==================== Summary DTO ====================

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AchievementSummary {
        private int totalAchievements;
        private int unlockedCount;
        private int claimableCount;
        private double completionPercentage;
    }
}
