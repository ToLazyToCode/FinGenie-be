package fingenie.com.fingenie.survey.service;

import fingenie.com.fingenie.survey.dto.*;
import fingenie.com.fingenie.survey.entity.*;
import fingenie.com.fingenie.survey.enums.BehavioralSegment;
import fingenie.com.fingenie.survey.enums.SurveySection;
import fingenie.com.fingenie.survey.enums.SurveyStatus;
import fingenie.com.fingenie.survey.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyService {

    private static final int SURVEY_EXPIRY_HOURS = 24;
    private static final int RETAKE_COOLDOWN_DAYS = 90;

    private final SurveyDefinitionRepository surveyDefinitionRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final UserSurveyResponseRepository surveyResponseRepository;
    private final UserBehaviorProfileRepository behaviorProfileRepository;
    private final ScoringEngine scoringEngine;
    private final SurveyEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public SurveyDefinitionResponse getActiveSurvey() {
        SurveyDefinition survey = surveyDefinitionRepository.findActiveSurvey()
            .orElseThrow(() -> new IllegalStateException("No active survey found"));

        List<SurveyQuestion> questions = surveyQuestionRepository.findAllWithAnswerOptions(survey.getId());
        return mapToSurveyDefinitionResponse(survey, questions);
    }

    @Transactional
    public StartSurveyResponse startSurvey(Long userId, StartSurveyRequest request) {
        log.info("Starting survey for user {}", userId);

        if (!Boolean.TRUE.equals(request.getConsentGiven())) {
            throw new IllegalArgumentException("Consent is required to start the survey");
        }

        Optional<UserSurveyResponse> existingInProgress = surveyResponseRepository.findInProgressSurvey(userId);

        if (existingInProgress.isPresent()) {
            UserSurveyResponse existing = existingInProgress.get();
            if (existing.getExpiresAt() == null || existing.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.info("Resuming existing in-progress survey for user {}", userId);
                return StartSurveyResponse.builder()
                    .responseId(existing.getId())
                    .surveyId(existing.getSurveyDefinition().getId())
                    .surveyVersion(existing.getSurveyDefinition().getVersion().toString())
                    .responseVersion(existing.getResponseVersion())
                    .startedAt(existing.getStartedAt())
                    .expiresAt(existing.getExpiresAt())
                    .status(existing.getStatus().name())
                    .existingAnswers(existing.getAnswers())
                    .build();
            }

            existing.markExpired();
            surveyResponseRepository.save(existing);
        }

        SurveyDefinition survey;
        if (request.getSurveyVersion() != null) {
            Long surveyVersion = parseSurveyVersion(request.getSurveyVersion());
            survey = surveyDefinitionRepository.findByVersion(surveyVersion)
                .orElseThrow(() -> new IllegalArgumentException("Survey version not found"));
        } else {
            survey = surveyDefinitionRepository.findActiveSurvey()
                .orElseThrow(() -> new IllegalStateException("No active survey found"));
        }

        surveyResponseRepository.markExistingAsSuperseded(userId);

        int nextVersion = surveyResponseRepository.getCurrentVersion(userId) + 1;

        UserSurveyResponse response = UserSurveyResponse.builder()
            .userId(userId)
            .surveyDefinition(survey)
            .responseVersion(nextVersion)
            .status(SurveyStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(SURVEY_EXPIRY_HOURS))
            .consentGiven(true)
            .consentTimestamp(LocalDateTime.now())
            .isLatest(true)
            .build();

        response = surveyResponseRepository.save(response);

        log.info("Created new survey response {} version {} for user {}", response.getId(), nextVersion, userId);

        return StartSurveyResponse.builder()
            .responseId(response.getId())
            .surveyId(survey.getId())
            .surveyVersion(survey.getVersion().toString())
            .responseVersion(nextVersion)
            .startedAt(response.getStartedAt())
            .expiresAt(response.getExpiresAt())
            .status(response.getStatus().name())
            .existingAnswers(new HashMap<>())
            .build();
    }

    @Transactional
    public SubmitSurveyResponse submitAnswers(Long userId, Long responseId, SubmitSurveyRequest request) {
        log.info("Submitting answers for user {} response {}", userId, responseId);

        UserSurveyResponse response = surveyResponseRepository.findById(responseId)
            .orElseThrow(() -> new IllegalArgumentException("Survey response not found"));

        if (!response.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Survey response does not belong to this user");
        }

        if (response.getStatus() != SurveyStatus.IN_PROGRESS) {
            throw new IllegalStateException("Survey is not in progress (status: " + response.getStatus() + ")");
        }

        if (response.getExpiresAt() != null && response.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.markExpired();
            surveyResponseRepository.save(response);
            throw new IllegalStateException("Survey has expired");
        }

        List<SurveyQuestion> questions = surveyQuestionRepository.findAllWithAnswerOptions(
            response.getSurveyDefinition().getId());
        validateSubmittedAnswers(questions, request.getAnswers());

        Map<String, String> currentAnswers = response.getAnswers();
        if (currentAnswers == null) {
            currentAnswers = new HashMap<>();
        }
        request.getAnswers().forEach(currentAnswers::put);
        response.setAnswers(currentAnswers);

        Set<String> requiredQuestionCodes = questions.stream()
            .filter(question -> Boolean.TRUE.equals(question.getIsRequired()))
            .map(SurveyQuestion::getQuestionCode)
            .collect(Collectors.toSet());

        int requiredCount = requiredQuestionCodes.size();
        long answeredRequiredCount = requiredQuestionCodes.stream().filter(currentAnswers::containsKey).count();

        boolean isComplete = answeredRequiredCount >= requiredCount;
        boolean isFinalSubmission = !Boolean.TRUE.equals(request.getIsPartialSubmission());

        if (isFinalSubmission) {
            if (!isComplete) {
                return SubmitSurveyResponse.builder()
                    .responseId(response.getId())
                    .status("INCOMPLETE")
                    .answeredQuestions(currentAnswers.size())
                    .totalQuestions(requiredCount)
                    .isComplete(false)
                    .message("Please answer all required questions before submitting")
                    .build();
            }

            response.markCompleted();
            surveyResponseRepository.save(response);
            UserBehaviorProfile profile = createOrUpdateBehaviorProfile(userId, response);
            eventPublisher.publishSurveyCompleted(userId, response.getId(), profile);

            log.info("Survey completed for user {}", userId);

            return SubmitSurveyResponse.builder()
                .responseId(response.getId())
                .status("COMPLETED")
                .answeredQuestions(currentAnswers.size())
                .totalQuestions(requiredCount)
                .isComplete(true)
                .completedAt(response.getCompletedAt())
                .message("Survey completed successfully!")
                .build();
        }

        surveyResponseRepository.save(response);

        return SubmitSurveyResponse.builder()
            .responseId(response.getId())
            .status("IN_PROGRESS")
            .answeredQuestions(currentAnswers.size())
            .totalQuestions(requiredCount)
            .isComplete(false)
            .message("Progress saved")
            .build();
    }

    @Transactional(readOnly = true)
    public SurveyStatusResponse getSurveyStatus(Long userId) {
        Optional<UserSurveyResponse> latestResponse = surveyResponseRepository.findByUserIdAndIsLatestTrue(userId);

        if (latestResponse.isEmpty()) {
            return SurveyStatusResponse.builder()
                .userId(userId)
                .hasSurvey(false)
                .hasCompletedSurvey(false)
                .currentStatus("NOT_STARTED")
                .canRetake(true)
                .message("Please complete the behavioral survey to get personalized insights")
                .build();
        }

        UserSurveyResponse response = latestResponse.get();
        boolean isCompleted = response.getStatus() == SurveyStatus.COMPLETED;

        Integer daysSinceCompletion = null;
        boolean canRetake = true;

        if (isCompleted && response.getCompletedAt() != null) {
            daysSinceCompletion = (int) ChronoUnit.DAYS.between(response.getCompletedAt(), LocalDateTime.now());
            canRetake = daysSinceCompletion >= RETAKE_COOLDOWN_DAYS;
        }

        return SurveyStatusResponse.builder()
            .userId(userId)
            .hasSurvey(true)
            .hasCompletedSurvey(isCompleted)
            .currentStatus(response.getStatus().name())
            .completedVersion(isCompleted ? response.getResponseVersion() : null)
            .completedAt(response.getCompletedAt())
            .canRetake(canRetake)
            .daysSinceCompletion(daysSinceCompletion)
            .message(buildStatusMessage(response.getStatus(), daysSinceCompletion, canRetake))
            .build();
    }

    @Transactional(readOnly = true)
    public BehaviorProfileResponse getBehaviorProfile(Long userId) {
        UserBehaviorProfile profile = behaviorProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "No behavior profile found. Please complete the survey first."));

        return mapToProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public boolean hasCompletedSurvey(Long userId) {
        return surveyResponseRepository.existsByUserIdAndStatus(userId, SurveyStatus.COMPLETED);
    }

    @Transactional
    public void deleteUserSurveyData(Long userId) {
        log.info("Deleting all survey data for user {} (GDPR request)", userId);
        behaviorProfileRepository.deleteByUserId(userId);
        surveyResponseRepository.deleteAllByUserId(userId);
    }

    private UserBehaviorProfile createOrUpdateBehaviorProfile(Long userId, UserSurveyResponse response) {
        ScoringEngine.ScoringResult result = scoringEngine.calculateScores(response.getAnswers());

        UserBehaviorProfile profile = behaviorProfileRepository.findByUserId(userId)
            .orElse(UserBehaviorProfile.builder()
                .userId(userId)
                .profileVersion(1)
                .build());

        if (profile.getId() != null) {
            profile.setProfileVersion(profile.getProfileVersion() + 1);
        }

        profile.setSurveyResponse(response);
        profile.updateScores(
            result.getOverspendingScore(),
            result.getDebtRiskScore(),
            result.getSavingsCapacityScore(),
            result.getFinancialAnxietyIndex()
        );
        profile.setSegment(result.getSegment());
        profile.setSegmentConfidence(result.getSegmentConfidence());
        profile.setFeatureVector(result.getFeatureVector());
        profile.setSurveyCompletedAt(response.getCompletedAt());

        Map<String, Object> explanationFactors = new HashMap<>();
        explanationFactors.put("topFactors", result.getTopFactors());
        explanationFactors.put("segmentReason", getSegmentReason(result.getSegment()));
        profile.setExplanationFactors(explanationFactors);

        profile.setFirstActionPlan(buildActionPlan(result.getSegment(), result));

        behaviorProfileRepository.save(profile);
        log.info("Saved behavior profile for user {} with segment {}", userId, result.getSegment());
        return profile;
    }

    private String getSegmentReason(BehavioralSegment segment) {
        return switch (segment) {
            case FINANCIALLY_AT_RISK -> "High debt risk combined with limited savings capacity";
            case IMPULSE_SPENDER -> "Frequent unplanned purchases affecting financial stability";
            case HIGH_EARNER_LOW_CONTROL -> "Good earning potential but spending exceeds optimal levels";
            case FINANCIALLY_ANXIOUS_BEGINNER -> "Financial stress with limited financial literacy";
            case STABLE_BUILDER -> "Consistent positive financial behaviors and habits";
            case MODERATE_MANAGER -> "Balanced approach with room for optimization";
        };
    }

    private Map<String, Object> buildActionPlan(BehavioralSegment segment, ScoringEngine.ScoringResult result) {
        List<BehaviorProfileResponse.ActionItem> actions = new ArrayList<>();

        switch (segment) {
            case FINANCIALLY_AT_RISK -> {
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Debt Assessment")
                    .description("Review all debts and create a prioritized payoff plan")
                    .priority("HIGH")
                    .category("DEBT")
                    .build());
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Emergency Buffer")
                    .description("Start small - save even 5% of income for emergencies")
                    .priority("HIGH")
                    .category("SAVING")
                    .build());
            }
            case IMPULSE_SPENDER -> {
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("24-Hour Rule")
                    .description("Wait 24 hours before any non-essential purchase over $50")
                    .priority("HIGH")
                    .category("SPENDING")
                    .build());
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Spending Alerts")
                    .description("Set up real-time notifications for transactions")
                    .priority("MEDIUM")
                    .category("SPENDING")
                    .build());
            }
            case HIGH_EARNER_LOW_CONTROL -> {
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Automated Savings")
                    .description("Set up automatic transfers to savings on payday")
                    .priority("HIGH")
                    .category("SAVING")
                    .build());
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Budget Categories")
                    .description("Create spending limits for discretionary categories")
                    .priority("MEDIUM")
                    .category("SPENDING")
                    .build());
            }
            case FINANCIALLY_ANXIOUS_BEGINNER -> {
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Financial Education")
                    .description("Complete a basic personal finance module")
                    .priority("HIGH")
                    .category("PLANNING")
                    .build());
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Start Small")
                    .description("Begin with one financial goal and track weekly")
                    .priority("MEDIUM")
                    .category("PLANNING")
                    .build());
            }
            case STABLE_BUILDER -> {
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Optimize Savings")
                    .description("Consider high-yield savings or investment options")
                    .priority("MEDIUM")
                    .category("SAVING")
                    .build());
                actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Long-term Goals")
                    .description("Review and adjust retirement contribution targets")
                    .priority("LOW")
                    .category("PLANNING")
                    .build());
            }
            case MODERATE_MANAGER -> actions.add(BehaviorProfileResponse.ActionItem.builder()
                    .title("Financial Review")
                    .description("Schedule monthly review of spending patterns")
                    .priority("MEDIUM")
                    .category("PLANNING")
                    .build());
        }

        Map<String, Object> plan = new HashMap<>();
        plan.put("actions", actions);
        plan.put("generatedAt", LocalDateTime.now().toString());
        return plan;
    }

    private void validateSubmittedAnswers(List<SurveyQuestion> questions, Map<String, String> answers) {
        Map<String, SurveyQuestion> questionByCode = questions.stream()
            .collect(Collectors.toMap(SurveyQuestion::getQuestionCode, q -> q));

        for (Map.Entry<String, String> answerEntry : answers.entrySet()) {
            String questionCode = answerEntry.getKey();
            String answerCode = answerEntry.getValue();

            SurveyQuestion question = questionByCode.get(questionCode);
            if (question == null) {
                throw new IllegalArgumentException("Unknown question code: " + questionCode);
            }

            boolean optionExists = question.getAnswerOptions().stream()
                .anyMatch(option -> option.getAnswerCode().equals(answerCode));

            if (!optionExists) {
                throw new IllegalArgumentException("Invalid answer code for question " + questionCode + ": " + answerCode);
            }
        }
    }

    private Long parseSurveyVersion(String surveyVersion) {
        try {
            return Long.parseLong(surveyVersion);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid survey version: " + surveyVersion);
        }
    }

    private String buildStatusMessage(SurveyStatus status, Integer daysSince, boolean canRetake) {
        return switch (status) {
            case NOT_STARTED -> "Please complete the behavioral survey to get personalized insights";
            case IN_PROGRESS -> "You have an in-progress survey. Continue where you left off.";
            case COMPLETED -> canRetake
                ? "You can retake the survey to update your profile"
                : String.format("Survey completed %d days ago. Retake available in %d days.",
                    daysSince, RETAKE_COOLDOWN_DAYS - daysSince);
            case EXPIRED -> "Your previous survey expired. Please start a new one.";
            case SUPERSEDED -> "Please complete your latest survey";
        };
    }

    private SurveyDefinitionResponse mapToSurveyDefinitionResponse(
            SurveyDefinition survey,
            List<SurveyQuestion> questions
    ) {
        Map<SurveySection, List<SurveyQuestion>> questionsBySection =
            questions.stream()
                .collect(Collectors.groupingBy(SurveyQuestion::getSectionCode));

        List<SurveyDefinitionResponse.SectionResponse> sections =
            Arrays.stream(SurveySection.values())
                .filter(questionsBySection::containsKey)
                .map(section -> SurveyDefinitionResponse.SectionResponse.builder()
                    .code(section.name())
                    .title(section.getTitle())
                    .order(section.getOrder())
                    .questions(questionsBySection.get(section).stream()
                        .map(this::mapQuestion)
                        .collect(Collectors.toList()))
                    .build())
                .collect(Collectors.toList());

        return SurveyDefinitionResponse.builder()
            .id(survey.getId())
            .version(survey.getVersion())
            .title(survey.getTitle())
            .description(survey.getDescription())
            .estimatedMinutes(survey.getEstimatedMinutes())
            .sections(sections)
            .build();
    }

    private SurveyDefinitionResponse.QuestionResponse mapQuestion(SurveyQuestion question) {
        return SurveyDefinitionResponse.QuestionResponse.builder()
            .id(question.getId())
            .questionCode(question.getQuestionCode())
            .questionText(question.getQuestionText())
            .order(question.getQuestionOrder())
            .isRequired(question.getIsRequired())
            .options(question.getAnswerOptions().stream()
                .map(opt -> SurveyDefinitionResponse.AnswerOptionResponse.builder()
                    .code(opt.getAnswerCode())
                    .text(opt.getAnswerText())
                    .order(opt.getDisplayOrder())
                    .build())
                .collect(Collectors.toList()))
            .build();
    }

    private BehaviorProfileResponse mapToProfileResponse(UserBehaviorProfile profile) {
        BehavioralSegment segment = profile.getSegment();

        @SuppressWarnings("unchecked")
        List<String> topFactors = profile.getExplanationFactors() != null
            ? (List<String>) profile.getExplanationFactors().get("topFactors")
            : List.of();

        Map<String, Object> actionPlan = profile.getFirstActionPlan();
        List<BehaviorProfileResponse.ActionItem> actions = new ArrayList<>();
        if (actionPlan != null && actionPlan.containsKey("actions")) {
            @SuppressWarnings("unchecked")
            List<Object> rawActions = (List<Object>) actionPlan.get("actions");
            actions = rawActions.stream()
                .map(this::toActionItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        return BehaviorProfileResponse.builder()
            .userId(profile.getUserId())
            .profileVersion(profile.getProfileVersion())
            .overspendingScore(profile.getOverspendingScore())
            .debtRiskScore(profile.getDebtRiskScore())
            .savingsCapacityScore(profile.getSavingsCapacityScore())
            .financialAnxietyIndex(profile.getFinancialAnxietyIndex())
            .segment(segment.name())
            .segmentDisplayName(segment.getDisplayName())
            .segmentDescription(segment.getDescription())
            .segmentConfidence(profile.getSegmentConfidence())
            .riskLevel(profile.getRiskLevel())
            .topFactors(topFactors)
            .suggestedActions(actions)
            .surveyCompletedAt(profile.getSurveyCompletedAt())
            .profileUpdatedAt(profile.getUpdatedAt().toLocalDateTime())
            .needsRefresh(profile.needsRefresh())
            .build();
    }

    private BehaviorProfileResponse.ActionItem toActionItem(Object rawAction) {
        if (rawAction instanceof BehaviorProfileResponse.ActionItem actionItem) {
            return actionItem;
        }

        if (rawAction instanceof Map<?, ?> map) {
            return BehaviorProfileResponse.ActionItem.builder()
                .title(Objects.toString(map.get("title"), null))
                .description(Objects.toString(map.get("description"), null))
                .priority(Objects.toString(map.get("priority"), null))
                .category(Objects.toString(map.get("category"), null))
                .build();
        }

        return null;
    }
}
