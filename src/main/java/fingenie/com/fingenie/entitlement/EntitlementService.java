package fingenie.com.fingenie.entitlement;

import fingenie.com.fingenie.ai.repository.AIChatMessageRepository;
import fingenie.com.fingenie.common.error.exceptions.AuthenticationExceptions;
import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.dto.EntitlementSnapshotResponse;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.UserSubscription;
import fingenie.com.fingenie.entity.UserSubscriptionStatus;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EntitlementService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String AI_CHAT_DAILY_LIMIT_KEY = "ai.chat.dailyLimit";
    private static final String AI_CHAT_DAILY_USAGE_KEY = "ai.chat.dailyUsage";
    private static final String AI_CHAT_DAILY_REMAINING_KEY = "ai.chat.dailyRemaining";

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final AccountRepository accountRepository;
    private final AIChatMessageRepository aiChatMessageRepository;

    @Transactional(readOnly = true)
    public EntitlementSnapshotResponse getSnapshot(Long accountId) {
        PlanTier planTier = resolvePlanTier(accountId);
        BillingPlan billingPlan = resolveBillingPlan(accountId, planTier);

        Map<String, Boolean> features = buildFeatures(planTier);
        long dailyLimit = getDailyChatLimit(planTier);
        long dailyUsage = getTodayUserChatMessageCount(accountId);
        long dailyRemaining = Math.max(0L, dailyLimit - dailyUsage);

        Map<String, Long> limits = new LinkedHashMap<>();
        limits.put(AI_CHAT_DAILY_LIMIT_KEY, dailyLimit);

        Map<String, Long> usage = new LinkedHashMap<>();
        usage.put(AI_CHAT_DAILY_USAGE_KEY, dailyUsage);

        Map<String, Long> remaining = new LinkedHashMap<>();
        remaining.put(AI_CHAT_DAILY_REMAINING_KEY, dailyRemaining);

        Map<String, Object> entitlements = new LinkedHashMap<>();
        entitlements.putAll(features);
        entitlements.put(AI_CHAT_DAILY_LIMIT_KEY, dailyLimit);

        return EntitlementSnapshotResponse.builder()
                .planTier(planTier)
                .billingPlan(billingPlan)
                .entitlements(entitlements)
                .features(features)
                .limits(limits)
                .usage(usage)
                .remaining(remaining)
                .build();
    }

    @Transactional(readOnly = true)
    public PlanTier resolvePlanTier(Long accountId) {
        Optional<UserSubscription> activeSubscription = findActiveSubscription(accountId);
        if (activeSubscription.isPresent() && activeSubscription.get().getPlan() != null) {
            BillingPlan billingPlan = BillingPlan.fromPlanCode(activeSubscription.get().getPlan().getPlanCode());
            return switch (billingPlan) {
                case PLUS_MONTHLY -> PlanTier.PLUS;
                case PREMIUM_MONTHLY, PREMIUM_YEARLY -> PlanTier.PREMIUM;
                case FREE -> PlanTier.FREE;
            };
        }

        boolean legacyPremium = accountRepository.findById(accountId)
                .map(Account::isPremium)
                .orElse(false);
        return legacyPremium ? PlanTier.PREMIUM : PlanTier.FREE;
    }

    @Transactional(readOnly = true)
    public boolean hasFeature(Long accountId, String featureKey) {
        EntitlementSnapshotResponse snapshot = getSnapshot(accountId);
        return Boolean.TRUE.equals(snapshot.getFeatures().get(featureKey));
    }

    @Transactional(readOnly = true)
    public void assertFeature(Long accountId, String featureKey) {
        if (!hasFeature(accountId, featureKey)) {
            throw new AuthenticationExceptions.AccessDeniedException(featureKey);
        }
    }

    @Transactional(readOnly = true)
    public void assertAiChatQuota(Long accountId) {
        EntitlementSnapshotResponse snapshot = getSnapshot(accountId);
        long limit = snapshot.getLimits().getOrDefault(AI_CHAT_DAILY_LIMIT_KEY, 0L);
        long used = snapshot.getUsage().getOrDefault(AI_CHAT_DAILY_USAGE_KEY, 0L);
        if (used >= limit) {
            throw new SystemExceptions.RateLimitExceededException(
                    "/api/v1/ai/conversations/chat",
                    (int) limit,
                    24 * 60 * 60
            );
        }
    }

    @Transactional(readOnly = true)
    public double getPersonalMissionPointMultiplier(Long accountId) {
        return switch (resolvePlanTier(accountId)) {
            case FREE -> 0.5d;
            case PLUS, PREMIUM -> 1.0d;
        };
    }

    @Transactional(readOnly = true)
    public double getGroupMissionPointMultiplier(Long accountId) {
        return switch (resolvePlanTier(accountId)) {
            case FREE -> 0.25d;
            case PLUS -> 0.5d;
            case PREMIUM -> 1.0d;
        };
    }

    private BillingPlan resolveBillingPlan(Long accountId, PlanTier planTier) {
        Optional<UserSubscription> activeSubscription = findActiveSubscription(accountId);
        if (activeSubscription.isPresent() && activeSubscription.get().getPlan() != null) {
            return BillingPlan.fromPlanCode(activeSubscription.get().getPlan().getPlanCode());
        }

        if (planTier == PlanTier.PREMIUM) {
            return BillingPlan.PREMIUM_MONTHLY;
        }
        if (planTier == PlanTier.PLUS) {
            return BillingPlan.PLUS_MONTHLY;
        }
        return BillingPlan.FREE;
    }

    private Optional<UserSubscription> findActiveSubscription(Long accountId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return userSubscriptionRepository.findByAccountId(accountId)
                .filter(sub -> sub.getStatus() == UserSubscriptionStatus.ACTIVE)
                .filter(sub -> sub.getEndsAt() != null && sub.getEndsAt().after(now));
    }

    private Map<String, Boolean> buildFeatures(PlanTier tier) {
        Map<String, Boolean> features = new LinkedHashMap<>();

        // Finance core
        features.put("analytics.day", true);
        features.put("analytics.week", true);
        features.put("analytics.month", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);
        features.put("analytics.year", tier == PlanTier.PREMIUM);
        features.put("savings.suggest.basic", true);
        features.put("savings.suggest.advanced", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);

        // AI / pet / coach
        features.put("ai.chat", true);
        features.put("ai.insight.basic", true);
        features.put("ai.insight.advanced", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);
        features.put("ai.insight.premium", tier == PlanTier.PREMIUM);
        features.put("premium.recommendations", tier == PlanTier.PREMIUM);

        // Mission / FinPoint / voucher
        features.put("mission.personal.points", true);
        features.put("mission.personal.points.full", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);
        features.put("mission.group.points", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);
        features.put("mission.group.points.full", tier == PlanTier.PREMIUM);
        features.put("voucher.personal.redeem", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);
        features.put("voucher.group.redeem", tier == PlanTier.PREMIUM);
        features.put("voucher.premium.pool", tier == PlanTier.PREMIUM);

        // Notification
        features.put("notification.basic", true);
        features.put("notification.smartReminder", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);
        features.put("notification.aiAlert", tier == PlanTier.PREMIUM);

        // Achievement
        features.put("achievement.basic", true);
        features.put("achievement.premium", tier == PlanTier.PLUS || tier == PlanTier.PREMIUM);

        return features;
    }

    private long getDailyChatLimit(PlanTier tier) {
        return switch (tier) {
            case FREE -> 10L;
            case PLUS -> 50L;
            case PREMIUM -> 200L;
        };
    }

    private long getTodayUserChatMessageCount(Long accountId) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        Timestamp fromInclusive = Timestamp.from(today.atStartOfDay(DEFAULT_ZONE).toInstant());
        Timestamp toExclusive = Timestamp.from(today.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant());
        return aiChatMessageRepository.countUserMessagesByAccountIdAndCreatedAtRange(
                accountId,
                fromInclusive,
                toExclusive
        );
    }
}
