package fingenie.com.fingenie.admin.controller;

import fingenie.com.fingenie.entity.Achievement;
import fingenie.com.fingenie.repository.AchievementRepository;
import fingenie.com.fingenie.repository.UserAchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin endpoints for Gamification / Achievement management.
 *
 * GET  /api/v1/admin/achievements          – list all achievements with earned counts
 * POST /api/v1/admin/achievements          – create new achievement
 * PUT  /api/v1/admin/achievements/{id}     – update achievement
 * DELETE /api/v1/admin/achievements/{id}   – soft-delete (deactivate) achievement
 */
@RestController
@RequestMapping("${api-prefix}/admin/achievements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGamificationController {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    // ── Response record ────────────────────────────────────────────────────

    public record AdminAchievementResponse(
            Long   id,
            String name,
            String description,
            String iconUrl,
            int    xpReward,
            String category,
            int    usersEarned
    ) {}

    // ── Request record ─────────────────────────────────────────────────────

    public record AchievementRequest(
            @NotBlank String name,
            String description,
            String iconUrl,
            @NotNull Integer xpReward,
            @NotBlank String category
    ) {}

    // ── Endpoints ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<AdminAchievementResponse>> listAll() {
        List<Achievement> achievements = achievementRepository.findAll();

        // Count users who earned each achievement
        Map<Long, Long> earnedCounts = userAchievementRepository.findAll().stream()
                .filter(ua -> Boolean.TRUE.equals(ua.getIsUnlocked()))
                .collect(Collectors.groupingBy(
                        ua -> ua.getAchievementId(),
                        Collectors.counting()
                ));

        List<AdminAchievementResponse> result = achievements.stream()
                .map(a -> new AdminAchievementResponse(
                        a.getId(),
                        a.getName(),
                        a.getDescription(),
                        a.getIcon(),
                        a.getXpReward() != null ? a.getXpReward() : 0,
                        a.getCategory() != null ? a.getCategory().name() : "MILESTONES",
                        earnedCounts.getOrDefault(a.getId(), 0L).intValue()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<AdminAchievementResponse> create(@Valid @RequestBody AchievementRequest req) {
        Achievement.AchievementCategory cat = parseCategory(req.category());

        Achievement a = Achievement.builder()
                .name(req.name())
                .description(req.description())
                .icon(req.iconUrl())
                .xpReward(req.xpReward() != null ? req.xpReward() : 10)
                .category(cat)
                .code(req.name().toLowerCase().replaceAll("[^a-z0-9]+", "_") + "_" + System.currentTimeMillis())
                .tier(Achievement.AchievementTier.BRONZE)
                .targetValue(1)
                .isActive(true)
                .isHidden(false)
                .sortOrder(0)
                .build();

        Achievement saved = achievementRepository.save(a);
        return ResponseEntity.ok(toResponse(saved, 0));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminAchievementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AchievementRequest req
    ) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + id));

        a.setName(req.name());
        a.setDescription(req.description());
        a.setIcon(req.iconUrl());
        a.setXpReward(req.xpReward() != null ? req.xpReward() : a.getXpReward());
        a.setCategory(parseCategory(req.category()));

        Achievement saved = achievementRepository.save(a);

        long earned = userAchievementRepository.findAll().stream()
                .filter(ua -> id.equals(ua.getAchievementId()) && Boolean.TRUE.equals(ua.getIsUnlocked()))
                .count();

        return ResponseEntity.ok(toResponse(saved, (int) earned));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + id));
        a.setIsActive(false);
        achievementRepository.save(a);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Achievement.AchievementCategory parseCategory(String raw) {
        try {
            return Achievement.AchievementCategory.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Achievement.AchievementCategory.MILESTONES;  // safe fallback
        }
    }

    private AdminAchievementResponse toResponse(Achievement a, int usersEarned) {
        return new AdminAchievementResponse(
                a.getId(),
                a.getName(),
                a.getDescription(),
                a.getIcon(),
                a.getXpReward() != null ? a.getXpReward() : 0,
                a.getCategory() != null ? a.getCategory().name() : "MILESTONES",
                usersEarned
        );
    }
}
