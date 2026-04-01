package fingenie.com.fingenie.survey.controller;

import fingenie.com.fingenie.survey.dto.*;
import fingenie.com.fingenie.survey.service.SurveyService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Behavioral Survey operations.
 */
@RestController
@RequestMapping("/api/v1/survey")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Behavioral Survey", description = "User onboarding behavioral survey APIs")
public class SurveyController {
    
    private final SurveyService surveyService;
    
    @GetMapping
    @Operation(summary = "Get active survey", description = "Retrieve the active survey definition with all questions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Survey retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No active survey found")
    })
    public ResponseEntity<SurveyDefinitionResponse> getActiveSurvey() {
        return ResponseEntity.ok(surveyService.getActiveSurvey());
    }
    
    @PostMapping("/start")
    @Operation(summary = "Start survey", description = "Start a new survey or resume an existing in-progress survey")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Survey started/resumed successfully"),
        @ApiResponse(responseCode = "400", description = "Consent not provided")
    })
    public ResponseEntity<StartSurveyResponse> startSurvey(
            @Parameter(description = "User ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) Long ignoredUserId,
            @Valid @RequestBody StartSurveyRequest request) {
        Long userId = SecurityUtils.getCurrentAccountId();
        log.info("Start survey request for user {}", userId);
        return ResponseEntity.ok(surveyService.startSurvey(userId, request));
    }
    
    @PostMapping("/{responseId}/submit")
    @Operation(summary = "Submit survey answers", description = "Submit partial or complete survey answers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Answers submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or survey not in progress"),
        @ApiResponse(responseCode = "404", description = "Survey response not found")
    })
    public ResponseEntity<SubmitSurveyResponse> submitAnswers(
            @Parameter(description = "User ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) Long ignoredUserId,
            @Parameter(description = "Survey response ID", required = true)
            @PathVariable Long responseId,
            @Valid @RequestBody SubmitSurveyRequest request) {
        Long userId = SecurityUtils.getCurrentAccountId();
        log.info("Submit answers request for user {} response {}", userId, responseId);
        return ResponseEntity.ok(surveyService.submitAnswers(userId, responseId, request));
    }
    
    @GetMapping("/status")
    @Operation(summary = "Get survey status", description = "Check user's survey completion status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    })
    public ResponseEntity<SurveyStatusResponse> getSurveyStatus(
            @Parameter(description = "User ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) Long ignoredUserId) {
        Long userId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(surveyService.getSurveyStatus(userId));
    }
    
    @DeleteMapping("/data")
    @Operation(summary = "Delete survey data (GDPR)", description = "Delete all survey data for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Data deleted successfully")
    })
    public ResponseEntity<Void> deleteSurveyData(
            @Parameter(description = "User ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) Long ignoredUserId) {
        Long userId = SecurityUtils.getCurrentAccountId();
        log.warn("GDPR deletion request for user {} survey data", userId);
        surveyService.deleteUserSurveyData(userId);
        return ResponseEntity.noContent().build();
    }
}
