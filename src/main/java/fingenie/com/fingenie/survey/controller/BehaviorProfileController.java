package fingenie.com.fingenie.survey.controller;

import fingenie.com.fingenie.survey.dto.BehaviorProfileResponse;
import fingenie.com.fingenie.survey.entity.UserBehaviorProfile;
import fingenie.com.fingenie.survey.repository.UserBehaviorProfileRepository;
import fingenie.com.fingenie.survey.service.ExplainabilityService;
import fingenie.com.fingenie.survey.service.SurveyService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User Behavior Profile operations.
 */
@RestController
@RequestMapping("/api/v1/behavior")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Behavior Profile", description = "User behavioral profile and insights APIs")
public class BehaviorProfileController {
    
    private final SurveyService surveyService;
    private final ExplainabilityService explainabilityService;
    private final UserBehaviorProfileRepository behaviorProfileRepository;
    
    @GetMapping("/profile")
    @Operation(summary = "Get behavior profile", 
               description = "Retrieve user's behavioral profile including scores, segment, and recommendations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No profile found - survey not completed")
    })
    public ResponseEntity<BehaviorProfileResponse> getBehaviorProfile(
            @Parameter(description = "User ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) Long ignoredUserId) {
        Long userId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(surveyService.getBehaviorProfile(userId));
    }
    
    @GetMapping("/insights")
    @Operation(summary = "Get profile insights", 
               description = "Get detailed explainable insights for user's behavioral profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Insights retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No profile found")
    })
    public ResponseEntity<ExplainabilityService.ProfileExplanation> getProfileInsights(
            @Parameter(description = "User ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) Long ignoredUserId) {
        Long userId = SecurityUtils.getCurrentAccountId();
        UserBehaviorProfile profile = behaviorProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("No behavior profile found. Please complete the survey first."));
        
        return ResponseEntity.ok(explainabilityService.explainProfile(profile));
    }
    
    @GetMapping("/check")
    @Operation(summary = "Check survey completion", 
               description = "Quick check if user has completed the behavioral survey")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Check completed")
    })
    public ResponseEntity<SurveyCompletionCheck> checkSurveyCompletion(
            @Parameter(description = "User ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) Long ignoredUserId) {
        Long userId = SecurityUtils.getCurrentAccountId();
        boolean hasCompleted = surveyService.hasCompletedSurvey(userId);
        return ResponseEntity.ok(new SurveyCompletionCheck(userId, hasCompleted));
    }
    
    /**
     * Simple DTO for survey completion check.
     */
    public record SurveyCompletionCheck(Long userId, boolean hasCompletedSurvey) {}
}
