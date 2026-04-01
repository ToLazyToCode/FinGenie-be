package fingenie.com.fingenie.survey.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Survey section codes for organizing questions.
 */
@Getter
@RequiredArgsConstructor
public enum SurveySection {
    SECTION_A("Income & Stability", 1),
    SECTION_B("Spending Behavior", 2),
    SECTION_C("Debt & Financial Pressure", 3),
    SECTION_D("Savings & Goals", 4),
    SECTION_E("Financial Discipline & Intent", 5);
    
    private final String title;
    private final int order;
}
