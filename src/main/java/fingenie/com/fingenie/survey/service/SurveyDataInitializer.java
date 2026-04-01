package fingenie.com.fingenie.survey.service;

import fingenie.com.fingenie.survey.entity.SurveyAnswerOption;
import fingenie.com.fingenie.survey.entity.SurveyDefinition;
import fingenie.com.fingenie.survey.entity.SurveyQuestion;
import fingenie.com.fingenie.survey.enums.SurveyAnswers.*;
import fingenie.com.fingenie.survey.enums.SurveySection;
import fingenie.com.fingenie.survey.repository.SurveyDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initializes the behavioral survey with questions and answer options.
 * Runs on application startup if no survey exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SurveyDataInitializer implements CommandLineRunner {
    
    private static final Long SURVEY_VERSION = 1L;
    
    private final SurveyDefinitionRepository surveyDefinitionRepository;
    
    @Override
    @Transactional
    public void run(String... args) {
        if (surveyDefinitionRepository.existsByVersion(SURVEY_VERSION)) {
            log.info("Survey version {} already exists, skipping initialization", SURVEY_VERSION);
            return;
        }
        
        log.info("Initializing behavioral survey version {}", SURVEY_VERSION);
        
        SurveyDefinition survey = SurveyDefinition.builder()
            .version(SURVEY_VERSION)
            .title("Financial Behavior Assessment")
            .description("Help us understand your financial habits to provide personalized recommendations")
            .isActiveSurvey(true)
            .estimatedMinutes(5)
            .build();
        
        AtomicInteger questionOrder = new AtomicInteger(1);
        
        // Section A: Income & Stability
        addQuestion(survey, SurveySection.SECTION_A, "A1", 
            "What is your primary source of income?",
            questionOrder.getAndIncrement(), new BigDecimal("1.0"), IncomeSource.class);
        
        addQuestion(survey, SurveySection.SECTION_A, "A2",
            "How would you describe your income stability?",
            questionOrder.getAndIncrement(), new BigDecimal("1.2"), IncomeStability.class);
        
        addQuestion(survey, SurveySection.SECTION_A, "A3",
            "What percentage of your income goes to fixed expenses (rent, utilities, loans)?",
            questionOrder.getAndIncrement(), new BigDecimal("1.0"), FixedExpenseRatio.class);
        
        // Section B: Spending Habits
        addQuestion(survey, SurveySection.SECTION_B, "B1",
            "How often do you make unplanned purchases?",
            questionOrder.getAndIncrement(), new BigDecimal("1.5"), UnplannedPurchaseFrequency.class);
        
        addQuestion(survey, SurveySection.SECTION_B, "B2",
            "When you feel stressed or emotional, how does it affect your spending?",
            questionOrder.getAndIncrement(), new BigDecimal("1.3"), EmotionalSpendingImpact.class);
        
        addQuestion(survey, SurveySection.SECTION_B, "B3",
            "Before making a large purchase, what do you typically do?",
            questionOrder.getAndIncrement(), new BigDecimal("1.2"), LargePurchaseApproach.class);
        
        addQuestion(survey, SurveySection.SECTION_B, "B4",
            "How often do you exceed your monthly budget?",
            questionOrder.getAndIncrement(), new BigDecimal("1.0"), BudgetExceedFrequency.class);
        
        // Section C: Debt & Obligations
        addQuestion(survey, SurveySection.SECTION_C, "C1",
            "How would you describe your current debt situation?",
            questionOrder.getAndIncrement(), new BigDecimal("1.5"), DebtSituation.class);
        
        addQuestion(survey, SurveySection.SECTION_C, "C2",
            "How often do you worry about paying bills?",
            questionOrder.getAndIncrement(), new BigDecimal("1.2"), BillWorryFrequency.class);
        
        addQuestion(survey, SurveySection.SECTION_C, "C3",
            "Do you ever borrow money or use credit cards to cover daily expenses?",
            questionOrder.getAndIncrement(), new BigDecimal("1.3"), BorrowForExpenses.class);
        
        // Section D: Savings & Goals
        addQuestion(survey, SurveySection.SECTION_D, "D1",
            "Do you have an emergency fund?",
            questionOrder.getAndIncrement(), new BigDecimal("1.5"), EmergencyFundStatus.class);
        
        addQuestion(survey, SurveySection.SECTION_D, "D2",
            "How consistently do you save money?",
            questionOrder.getAndIncrement(), new BigDecimal("1.3"), SavingsConsistency.class);
        
        addQuestion(survey, SurveySection.SECTION_D, "D3",
            "What is your primary financial goal?",
            questionOrder.getAndIncrement(), new BigDecimal("0.8"), PrimaryFinancialGoal.class);
        
        addQuestion(survey, SurveySection.SECTION_D, "D4",
            "How confident are you in achieving your financial goals?",
            questionOrder.getAndIncrement(), new BigDecimal("1.0"), GoalConfidence.class);
        
        // Section E: Financial Literacy & Awareness
        addQuestion(survey, SurveySection.SECTION_E, "E1",
            "How do you track your expenses?",
            questionOrder.getAndIncrement(), new BigDecimal("1.0"), ExpenseTrackingLevel.class);
        
        addQuestion(survey, SurveySection.SECTION_E, "E2",
            "How often do you review your financial situation?",
            questionOrder.getAndIncrement(), new BigDecimal("0.8"), FinancialReviewFrequency.class);
        
        addQuestion(survey, SurveySection.SECTION_E, "E3",
            "How would you rate your financial knowledge?",
            questionOrder.getAndIncrement(), new BigDecimal("0.8"), FinancialKnowledgeLevel.class);
        
        surveyDefinitionRepository.save(survey);
        log.info("Behavioral survey initialized with {} questions", survey.getQuestions().size());
    }
    
    private <E extends Enum<E>> void addQuestion(
            SurveyDefinition survey, 
            SurveySection section,
            String questionCode,
            String questionText,
            int order,
            BigDecimal weight,
            Class<E> answerEnumClass) {
        
        SurveyQuestion question = SurveyQuestion.builder()
            .sectionCode(section)
            .questionCode(questionCode)
            .questionText(questionText)
            .questionOrder(order)
            .weight(weight)
            .isRequired(true)
            .answerEnumClass(answerEnumClass.getSimpleName())
            .build();
        
        // Add answer options from enum
        AtomicInteger displayOrder = new AtomicInteger(1);
        for (E enumValue : answerEnumClass.getEnumConstants()) {
            try {
                String displayText = (String) answerEnumClass.getMethod("getDisplayText").invoke(enumValue);
                int scoreValue = (int) answerEnumClass.getMethod("getScoreValue").invoke(enumValue);
                
                SurveyAnswerOption option = SurveyAnswerOption.builder()
                    .answerCode(enumValue.name())
                    .answerText(displayText)
                    .scoreValue(scoreValue)
                    .displayOrder(displayOrder.getAndIncrement())
                    .build();
                
                question.addAnswerOption(option);
            } catch (Exception e) {
                log.warn("Could not extract answer option from enum {}: {}", enumValue, e.getMessage());
            }
        }
        
        survey.addQuestion(question);
    }
}
