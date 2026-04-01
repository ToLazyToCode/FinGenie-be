package fingenie.com.fingenie.survey.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All survey answer options with their scoring values.
 * 
 * Score values are on a 0-100 scale where:
 * - Higher = Better financial health (for positive indicators)
 * - Lower = More risk (inverted when calculating risk scores)
 */
public class SurveyAnswers {
    
    // ====== SECTION A: Income & Stability ======
    
    @Getter
    @RequiredArgsConstructor
    public enum IncomeSource {
        SALARY("Salaried Employee", 80),
        FREELANCE("Freelancer/Contractor", 60),
        BUSINESS("Business Owner", 50),
        INVESTMENT("Investment Income", 70),
        MULTIPLE("Multiple Income Sources", 75),
        IRREGULAR("Irregular/Gig Work", 30);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, IncomeSource::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum IncomeStability {
        VERY_STABLE("Very stable - same amount each month", 100),
        MOSTLY_STABLE("Mostly stable - minor variations", 75),
        SOMEWHAT_VARIABLE("Somewhat variable - noticeable fluctuations", 45),
        HIGHLY_VARIABLE("Highly variable - unpredictable", 20);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, IncomeStability::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum FixedExpenseRatio {
        UNDER_30("Under 30%", 100),
        BETWEEN_30_50("30% - 50%", 70),
        BETWEEN_50_70("50% - 70%", 40),
        OVER_70("Over 70%", 15);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, FixedExpenseRatio::getScoreValue));
        }
    }
    
    // ====== SECTION B: Spending Behavior ======
    
    @Getter
    @RequiredArgsConstructor
    public enum UnplannedPurchaseFrequency {
        RARELY("Rarely - almost never", 100),
        OCCASIONALLY("Occasionally - once or twice a month", 70),
        FREQUENTLY("Frequently - weekly", 35),
        VERY_OFTEN("Very often - multiple times a week", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, UnplannedPurchaseFrequency::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum EmotionalSpendingImpact {
        DECREASES("Decreases - I spend less when stressed", 100),
        NO_CHANGE("No change - emotions don't affect spending", 80),
        SLIGHT_INCREASE("Slight increase - minor comfort purchases", 40),
        SIGNIFICANT_INCREASE("Significant increase - retail therapy", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, EmotionalSpendingImpact::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum LargePurchaseApproach {
        SAVED_IN_ADVANCE("Save in advance - pay in full", 100),
        PARTIAL_SAVINGS("Partial savings + some credit", 70),
        CREDIT_THEN_PAY("Credit first, pay off quickly", 35),
        FULL_CREDIT("Full credit - pay minimum", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, LargePurchaseApproach::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum BudgetExceedFrequency {
        NEVER("Never", 100),
        RARELY("Rarely - once every few months", 80),
        SOMETIMES("Sometimes - about monthly", 50),
        OFTEN("Often - most months", 25),
        ALWAYS("Always - every month", 5);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, BudgetExceedFrequency::getScoreValue));
        }
    }
    
    // ====== SECTION C: Debt & Financial Pressure ======
    
    @Getter
    @RequiredArgsConstructor
    public enum DebtSituation {
        NO_DEBT("No debt", 100),
        LOW_MANAGEABLE("Low and manageable", 75),
        MODERATE("Moderate - working on it", 50),
        HIGH_STRUGGLING("High - struggling to manage", 25),
        OVERWHELMING("Overwhelming - can't keep up", 5);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, DebtSituation::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum BillWorryFrequency {
        NEVER("Never", 100),
        RARELY("Rarely", 80),
        SOMETIMES("Sometimes", 50),
        OFTEN("Often", 25),
        ALWAYS("Always - constant worry", 5);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, BillWorryFrequency::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum BorrowForExpenses {
        NEVER("Never", 100),
        ONCE_TWICE("Once or twice ever", 70),
        OCCASIONALLY("Occasionally", 40),
        FREQUENTLY("Frequently", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, BorrowForExpenses::getScoreValue));
        }
    }
    
    // ====== SECTION D: Savings & Goals ======
    
    @Getter
    @RequiredArgsConstructor
    public enum EmergencyFundStatus {
        YES_6_MONTHS_PLUS("Yes - 6+ months of expenses", 100),
        YES_3_6_MONTHS("Yes - 3-6 months of expenses", 75),
        YES_UNDER_3_MONTHS("Yes - under 3 months", 50),
        NO_BUILDING("No - but building one", 30),
        NO_NONE("No - none at all", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, EmergencyFundStatus::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum SavingsConsistency {
        ALWAYS("Always - without fail", 100),
        USUALLY("Usually - most months", 75),
        SOMETIMES("Sometimes - when possible", 50),
        RARELY("Rarely - hard to save", 25),
        NEVER("Never - nothing left to save", 5);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, SavingsConsistency::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum PrimaryFinancialGoal {
        EMERGENCY_FUND("Build emergency fund", 80),
        DEBT_PAYOFF("Pay off debt", 60),
        MAJOR_PURCHASE("Save for major purchase", 70),
        INVESTMENT("Grow investments", 75),
        RETIREMENT("Retirement savings", 85),
        NO_CLEAR_GOAL("No clear goal", 30);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, PrimaryFinancialGoal::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum GoalConfidence {
        VERY_CONFIDENT("Very confident", 100),
        SOMEWHAT_CONFIDENT("Somewhat confident", 70),
        UNCERTAIN("Uncertain", 45),
        NOT_CONFIDENT("Not confident", 20),
        NO_GOALS("No specific goals", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, GoalConfidence::getScoreValue));
        }
    }
    
    // ====== SECTION E: Financial Discipline & Intent ======
    
    @Getter
    @RequiredArgsConstructor
    public enum ExpenseTrackingLevel {
        DETAILED_TRACKING("Detailed - track every expense", 100),
        BASIC_TRACKING("Basic - track major expenses", 70),
        OCCASIONAL_CHECK("Occasional - check bank statements", 40),
        NO_TRACKING("No tracking at all", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, ExpenseTrackingLevel::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum FinancialReviewFrequency {
        WEEKLY("Weekly", 100),
        MONTHLY("Monthly", 75),
        QUARTERLY("Every few months", 50),
        RARELY("Rarely", 25),
        NEVER("Never", 5);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, FinancialReviewFrequency::getScoreValue));
        }
    }
    
    @Getter
    @RequiredArgsConstructor
    public enum FinancialKnowledgeLevel {
        EXPERT("Expert - very knowledgeable", 100),
        GOOD("Good - understand most concepts", 75),
        BASIC("Basic - know the fundamentals", 50),
        LIMITED("Limited - learning the basics", 25),
        VERY_LIMITED("Very limited - just starting", 10);
        
        private final String displayText;
        private final int scoreValue;
        
        public static Map<String, Integer> getScoreMap() {
            return Arrays.stream(values())
                    .collect(Collectors.toMap(Enum::name, FinancialKnowledgeLevel::getScoreValue));
        }
    }
}
