package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.service.NotificationDeviceTokenService;
import fingenie.com.fingenie.service.NotificationService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDeviceTokenService notificationDeviceTokenService;

    @GetMapping
    @Operation(summary = "Get notifications", description = "Get paginated notifications for current user")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(notificationService.getNotifications(accountId, page, size));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent notifications", description = "Get last 50 notifications")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(notificationService.getRecentNotifications(accountId));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Get all unread notifications sorted by priority")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(accountId));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Count unread notifications", description = "Get count of unread notifications")
    public ResponseEntity<Map<String, Long>> countUnread() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        long count = notificationService.countUnread(accountId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark a specific notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        notificationService.markAsRead(accountId, notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        int count = notificationService.markAllAsRead(accountId);
        return ResponseEntity.ok(Map.of("markedCount", count));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification", description = "Delete a specific notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        notificationService.deleteNotification(accountId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get preferences", description = "Get notification preferences for current user")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(notificationService.getPreferences(accountId));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update preferences", description = "Update notification preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @RequestBody UpdatePreferenceRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(notificationService.updatePreferences(accountId, request));
    }

    @PostMapping("/device-token")
    @Operation(summary = "Register or update notification device token")
    public ResponseEntity<NotificationDeviceTokenResponse> registerDeviceToken(
            @Valid @RequestBody NotificationDeviceTokenRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(notificationDeviceTokenService.registerOrUpdate(accountId, request));
    }

    @DeleteMapping("/device-token")
    @Operation(summary = "Disable notification device token")
    public ResponseEntity<Void> disableDeviceToken(@RequestParam("deviceToken") String deviceToken) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        notificationDeviceTokenService.disableToken(accountId, deviceToken);
        return ResponseEntity.noContent().build();
    }
}
