package fingenie.com.fingenie.ai.controller;

import fingenie.com.fingenie.ai.service.AIGamificationIntegrationService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("${api-prefix}/ai/gamification")
@RequiredArgsConstructor
@Tag(name = "AI Gamification", description = "AI-powered gamification features")
public class GamificationAIController {

    private final AIGamificationIntegrationService gamificationAI;

    @Operation(summary = "Award XP for accepted prediction", description = "AI awards XP when user accepts a prediction")
    @PostMapping("/xp/prediction-accepted/{userId}/{predictionId}")
    public ResponseEntity<Void> awardXPPredictionAccepted(
            @PathVariable Long userId, 
            @PathVariable Long predictionId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        gamificationAI.awardXPPredictionAccepted(accountId, predictionId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Award XP for AI-assisted streak", description = "AI awards XP for maintaining streak with AI help")
    @PostMapping("/xp/ai-streak/{userId}/{streakDays}")
    public ResponseEntity<Void> awardXPAIStreakHelp(
            @PathVariable Long userId, 
            @PathVariable int streakDays) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        gamificationAI.awardXPAIStreakHelp(accountId, streakDays);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Award XP for savings milestone", description = "AI awards XP for reaching savings goals with AI guidance")
    @PostMapping("/xp/savings-milestone/{userId}")
    public ResponseEntity<Void> awardXPSavingsMilestone(
            @PathVariable Long userId,
            @RequestParam BigDecimal savedAmount,
            @RequestParam String milestoneType) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        gamificationAI.awardXPSavingsMilestone(accountId, savedAmount, milestoneType);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Process AI learning milestone", description = "AI awards XP when confidence improves")
    @PostMapping("/xp/learning-milestone/{userId}")
    public ResponseEntity<Void> processAILearningMilestone(
            @PathVariable Long userId,
            @RequestParam BigDecimal oldConfidence,
            @RequestParam BigDecimal newConfidence) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        gamificationAI.processAILearningMilestone(accountId, oldConfidence, newConfidence);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Award XP for feedback", description = "AI awards XP for providing feedback to improve predictions")
    @PostMapping("/xp/feedback/{userId}")
    public ResponseEntity<Void> awardXPFeedbackProvided(
            @PathVariable Long userId,
            @RequestParam String feedbackType) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        gamificationAI.awardXPFeedbackProvided(accountId, feedbackType);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get AI gamification message", description = "Generate AI-powered motivational messages")
    @GetMapping("/message/{userId}/{context}")
    public ResponseEntity<String> getGamificationMessage(
            @PathVariable Long userId,
            @PathVariable String context) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        String message = gamificationAI.generateGamificationMessage(accountId, context);
        return ResponseEntity.ok(message);
    }
}
