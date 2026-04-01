package fingenie.com.fingenie.ai.controller;

import fingenie.com.fingenie.ai.dto.UserAISpendingProfileResponse;
import fingenie.com.fingenie.repository.UserAISpendingProfileRepository;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for AI spending profile management.
 * OSIV-SAFE: All endpoints return DTOs, not entities.
 */
@RestController
@RequestMapping("${api-prefix}/ai/profile")
@RequiredArgsConstructor
@Tag(name = "AI Profile", description = "AI user profile and learning data")
public class AIProfileController {

    private final UserAISpendingProfileRepository profileRepository;

    @Operation(summary = "Get AI spending profile for user", description = "Returns the learned spending patterns and prediction accuracy")
    @GetMapping("/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserAISpendingProfileResponse> getProfile(@PathVariable Long userId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return profileRepository.findByUserId(accountId)
                .map(UserAISpendingProfileResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Reset AI profile for user", description = "Clears learned data for a fresh start")
    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> resetProfile(@PathVariable Long userId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        profileRepository.deleteByUserId(accountId);
        return ResponseEntity.ok().build();
    }
}
