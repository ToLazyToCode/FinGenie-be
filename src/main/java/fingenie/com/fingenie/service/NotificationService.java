package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.entity.Notification;
import fingenie.com.fingenie.entity.NotificationPreference;
import fingenie.com.fingenie.repository.NotificationPreferenceRepository;
import fingenie.com.fingenie.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    // Notification Types
    public static final String TYPE_BUDGET_WARNING = "BUDGET_WARNING";
    public static final String TYPE_BUDGET_EXCEEDED = "BUDGET_EXCEEDED";
    public static final String TYPE_STREAK_REMINDER = "STREAK_REMINDER";
    public static final String TYPE_STREAK_LOST = "STREAK_LOST";
    public static final String TYPE_STREAK_MILESTONE = "STREAK_MILESTONE";
    public static final String TYPE_GUESS_READY = "GUESS_READY";
    public static final String TYPE_GOAL_PROGRESS = "GOAL_PROGRESS";
    public static final String TYPE_GOAL_COMPLETED = "GOAL_COMPLETED";
    public static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String TYPE_FRIEND_ACCEPTED = "FRIEND_ACCEPTED";
    public static final String TYPE_FRIEND_MESSAGE = "FRIEND_MESSAGE";
    public static final String TYPE_SHARED_PIGGY_INVITATION = "SHARED_PIGGY_INVITATION";
    public static final String TYPE_ACHIEVEMENT_UNLOCKED = "ACHIEVEMENT_UNLOCKED";
    public static final String TYPE_LEVEL_UP = "LEVEL_UP";
    public static final String TYPE_PET_MOOD = "PET_MOOD";
    public static final String TYPE_DAILY_SUMMARY = "DAILY_SUMMARY";

    // Action Types
    public static final String ACTION_OPEN_SCREEN = "OPEN_SCREEN";
    public static final String ACTION_DEEP_LINK = "DEEP_LINK";
    public static final String ACTION_DISMISS = "DISMISS";

    /**
     * Create a notification for a user.
     */
    @Transactional
    public Notification createNotification(Long accountId, String type, String title, String body,
                                           String actionType, String actionData, Integer priority,
                                           LocalDateTime expiresAt) {
        // Check user preferences
        if (!shouldSendNotification(accountId, type)) {
            log.debug("Notification of type {} suppressed for user {} due to preferences", type, accountId);
            return null;
        }

        Notification notification = Notification.builder()
                .accountId(accountId)
                .type(type)
                .title(title)
                .body(body)
                .isRead(false)
                .actionType(actionType)
                .actionData(actionData)
                .priority(priority != null ? priority : 0)
                .expiresAt(expiresAt)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created notification {} of type {} for user {}", notification.getId(), type, accountId);

        // TODO: Trigger push notification via Firebase/APNS
        triggerPushNotification(notification);

        return notification;
    }

    /**
     * Helper method to create simple notifications.
     */
    @Transactional
    public Notification createSimpleNotification(Long accountId, String type, String title, String body) {
        return createNotification(accountId, type, title, body, ACTION_DISMISS, null, 0, null);
    }

    /**
     * Create a notification with screen navigation action.
     */
    @Transactional
    public Notification createNavigationNotification(Long accountId, String type, String title, String body,
                                                     String screenName, Integer priority) {
        return createNotification(accountId, type, title, body, ACTION_OPEN_SCREEN, screenName, priority, null);
    }

    /**
     * Get paginated notifications for a user.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long accountId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size));
        return notifications.map(this::toResponse);
    }

    /**
     * Get recent notifications (last 50).
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(Long accountId) {
        return notificationRepository.findTop50ByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get unread notifications.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long accountId) {
        return notificationRepository.findUnreadByAccountId(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Count unread notifications.
     */
    @Transactional(readOnly = true)
    public long countUnread(Long accountId) {
        return notificationRepository.countUnreadByAccountId(accountId);
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(Long accountId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getAccountId().equals(accountId)) {
            throw new SecurityException("Cannot modify another user's notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read.
     */
    @Transactional
    public int markAllAsRead(Long accountId) {
        return notificationRepository.markAllAsReadByAccountId(accountId);
    }

    /**
     * Delete a notification.
     */
    @Transactional
    public void deleteNotification(Long accountId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getAccountId().equals(accountId)) {
            throw new SecurityException("Cannot delete another user's notification");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Get user's notification preferences.
     */
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(Long accountId) {
        NotificationPreference pref = preferenceRepository.findByAccountId(accountId)
                .orElse(createDefaultPreferences(accountId));
        return toPreferenceResponse(pref);
    }

    /**
     * Update user's notification preferences.
     */
    @Transactional
    public NotificationPreferenceResponse updatePreferences(Long accountId, UpdatePreferenceRequest request) {
        NotificationPreference pref = preferenceRepository.findByAccountId(accountId)
                .orElse(createDefaultPreferences(accountId));

        if (request.getPushEnabled() != null) pref.setPushEnabled(request.getPushEnabled());
        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getBudgetAlerts() != null) pref.setBudgetAlerts(request.getBudgetAlerts());
        if (request.getStreakReminders() != null) pref.setStreakReminders(request.getStreakReminders());
        if (request.getGuessPrompts() != null) pref.setGuessPrompts(request.getGuessPrompts());
        if (request.getGoalProgress() != null) pref.setGoalProgress(request.getGoalProgress());
        if (request.getFriendActivity() != null) pref.setFriendActivity(request.getFriendActivity());
        if (request.getAchievementUnlocks() != null) pref.setAchievementUnlocks(request.getAchievementUnlocks());
        if (request.getDailySummary() != null) pref.setDailySummary(request.getDailySummary());

        pref = preferenceRepository.save(pref);
        return toPreferenceResponse(pref);
    }

    /**
     * Cleanup expired and old notifications.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupNotifications() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = notificationRepository.deleteExpiredNotifications(now);
        log.info("Deleted {} expired notifications", expiredCount);
    }

    // ==================== Private Helper Methods ====================

    private boolean shouldSendNotification(Long accountId, String type) {
        NotificationPreference pref = preferenceRepository.findByAccountId(accountId).orElse(null);
        if (pref == null) return true; // Default: send all

        if (!pref.getPushEnabled()) return false;

        return switch (type) {
            case TYPE_BUDGET_WARNING, TYPE_BUDGET_EXCEEDED -> pref.getBudgetAlerts();
            case TYPE_STREAK_REMINDER, TYPE_STREAK_LOST, TYPE_STREAK_MILESTONE -> pref.getStreakReminders();
            case TYPE_GUESS_READY -> pref.getGuessPrompts();
            case TYPE_GOAL_PROGRESS, TYPE_GOAL_COMPLETED -> pref.getGoalProgress();
            case TYPE_FRIEND_REQUEST, TYPE_FRIEND_ACCEPTED, TYPE_FRIEND_MESSAGE, TYPE_SHARED_PIGGY_INVITATION -> pref.getFriendActivity();
            case TYPE_ACHIEVEMENT_UNLOCKED, TYPE_LEVEL_UP -> pref.getAchievementUnlocks();
            case TYPE_DAILY_SUMMARY -> pref.getDailySummary();
            default -> true;
        };
    }

    private NotificationPreference createDefaultPreferences(Long accountId) {
        return NotificationPreference.builder()
                .accountId(accountId)
                .pushEnabled(true)
                .emailEnabled(false)
                .budgetAlerts(true)
                .streakReminders(true)
                .guessPrompts(true)
                .goalProgress(true)
                .friendActivity(true)
                .achievementUnlocks(true)
                .dailySummary(false)
                .build();
    }

    private void triggerPushNotification(Notification notification) {
        // TODO: Implement Firebase Cloud Messaging integration
        // This would call FCM API to send push notification to user's devices
        log.debug("Push notification would be sent for notification {}", notification.getId());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .isRead(notification.getIsRead())
                .actionType(notification.getActionType())
                .actionData(notification.getActionData())
                .priority(notification.getPriority())
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }

    private NotificationPreferenceResponse toPreferenceResponse(NotificationPreference pref) {
        return NotificationPreferenceResponse.builder()
                .pushEnabled(pref.getPushEnabled())
                .emailEnabled(pref.getEmailEnabled())
                .budgetAlerts(pref.getBudgetAlerts())
                .streakReminders(pref.getStreakReminders())
                .guessPrompts(pref.getGuessPrompts())
                .goalProgress(pref.getGoalProgress())
                .friendActivity(pref.getFriendActivity())
                .achievementUnlocks(pref.getAchievementUnlocks())
                .dailySummary(pref.getDailySummary())
                .build();
    }

    // ==================== Convenience Methods for Specific Notifications ====================

    public void notifyBudgetWarning(Long accountId, String categoryName, double percentUsed) {
        String title = "Budget Alert";
        String body = String.format("You've used %.0f%% of your %s budget", percentUsed, categoryName);
        createNavigationNotification(accountId, TYPE_BUDGET_WARNING, title, body, "Analytics", 2);
    }

    public void sendBudgetWarning(Long accountId, String categoryName, int percentUsed) {
        notifyBudgetWarning(accountId, categoryName, (double) percentUsed);
    }

    public void sendBudgetExceeded(Long accountId, String categoryName, java.math.BigDecimal exceededBy) {
        String title = "Budget Exceeded!";
        String body = String.format("You've exceeded your %s budget by $%.2f", categoryName, exceededBy.doubleValue());
        createNavigationNotification(accountId, TYPE_BUDGET_EXCEEDED, title, body, "Analytics", 3);
    }

    public void notifyStreakReminder(Long accountId, int currentStreak) {
        String title = "Don't Break Your Streak!";
        String body = String.format("Log a transaction to keep your %d-day streak alive!", currentStreak);
        createNavigationNotification(accountId, TYPE_STREAK_REMINDER, title, body, "Home", 2);
    }

    public void notifyAchievementUnlocked(Long accountId, String achievementName, int xpReward) {
        String title = "Achievement Unlocked!";
        String body = String.format("You earned '%s' and gained %d XP!", achievementName, xpReward);
        createNavigationNotification(accountId, TYPE_ACHIEVEMENT_UNLOCKED, title, body, "Achievements", 1);
    }

    public void notifyLevelUp(Long accountId, int newLevel) {
        String title = "Level Up!";
        String body = String.format("Congratulations! You've reached Level %d!", newLevel);
        createNavigationNotification(accountId, TYPE_LEVEL_UP, title, body, "Profile", 1);
    }

    public void notifyFriendRequest(Long accountId, String friendName) {
        String title = "New Friend Request";
        String body = String.format("%s wants to be your friend!", friendName);
        createNavigationNotification(accountId, TYPE_FRIEND_REQUEST, title, body, "Friends", 1);
    }

    public void notifyGuessReady(Long accountId) {
        String title = "Guess Your Spending!";
        String body = "A new spending guess is ready. How close can you get?";
        createNavigationNotification(accountId, TYPE_GUESS_READY, title, body, "Home", 1);
    }
}
