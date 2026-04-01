package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.AchievementResponse;
import fingenie.com.fingenie.entity.Achievement.AchievementCategory;
import fingenie.com.fingenie.service.AchievementService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@Tag(name = "Achievements", description = "Achievement management endpoints")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    @Operation(summary = "Get all achievements", description = "Get all achievements with user progress")
    public ResponseEntity<List<AchievementResponse>> getAllAchievements() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(achievementService.getAllAchievementsForUser(accountId));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get by category", description = "Get achievements by category with progress")
    public ResponseEntity<List<AchievementResponse>> getByCategory(@PathVariable String category) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        AchievementCategory cat = AchievementCategory.valueOf(category.toUpperCase());
        return ResponseEntity.ok(achievementService.getAchievementsByCategory(accountId, cat));
    }

    @GetMapping("/claimable")
    @Operation(summary = "Get claimable", description = "Get unlocked but unclaimed achievements")
    public ResponseEntity<List<AchievementResponse>> getClaimable() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(achievementService.getClaimableAchievements(accountId));
    }

    @PostMapping("/{achievementId}/claim")
    @Operation(summary = "Claim achievement", description = "Claim XP reward for unlocked achievement")
    public ResponseEntity<AchievementResponse> claimAchievement(@PathVariable Long achievementId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(achievementService.claimAchievement(accountId, achievementId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get summary", description = "Get achievement summary for dashboard")
    public ResponseEntity<AchievementService.AchievementSummary> getSummary() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(achievementService.getSummary(accountId));
    }
}
