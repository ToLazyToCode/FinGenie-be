package fingenie.com.fingenie.survey.service;

import fingenie.com.fingenie.survey.entity.UserBehaviorProfile;
import fingenie.com.fingenie.survey.enums.BehavioralSegment;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for generating explainable insights from behavioral profiles.
 * 
 * Provides:
 * - Natural language explanations of risk scores
 * - Top contributing factors
 * - Actionable recommendations
 * - Segment-specific messaging
 */
@Service
@Slf4j
public class ExplainabilityService {
    
    /**
     * Generate a complete explanation for a user's behavior profile.
     */
    public ProfileExplanation explainProfile(UserBehaviorProfile profile) {
        String overallSummary = generateOverallSummary(profile);
        List<ScoreExplanation> scoreInsights = explainScores(profile);
        String segmentRationale = explainSegment(profile.getSegment(), profile);
        List<String> topFactors = extractTopFactors(profile);
        List<RecommendedAction> recommendations = generateRecommendations(profile);
        
        return ProfileExplanation.builder()
            .overallSummary(overallSummary)
            .riskLevel(profile.getRiskLevel())
            .scoreInsights(scoreInsights)
            .segmentRationale(segmentRationale)
            .topFactors(topFactors)
            .recommendations(recommendations)
            .build();
    }
    
    /**
     * Generate an overall summary of the user's financial health.
     */
    private String generateOverallSummary(UserBehaviorProfile profile) {
        BehavioralSegment segment = profile.getSegment();
        String riskLevel = profile.getRiskLevel();
        
        StringBuilder summary = new StringBuilder();
        
        summary.append("Based on your responses, ");
        
        switch (segment) {
            case FINANCIALLY_AT_RISK -> 
                summary.append("we've identified some areas where you may benefit from immediate attention. " +
                    "Your financial situation shows signs of stress, but with the right steps, " +
                    "you can start building a more secure foundation.");
            
            case IMPULSE_SPENDER -> 
                summary.append("your spending patterns suggest you might benefit from strategies to " +
                    "manage unplanned purchases. The good news is that small behavioral changes " +
                    "can have a significant impact on your financial health.");
            
            case HIGH_EARNER_LOW_CONTROL -> 
                summary.append("you have strong earning potential, but there's an opportunity to " +
                    "optimize how you manage your money. With better spending controls, " +
                    "you could accelerate your financial goals significantly.");
            
            case FINANCIALLY_ANXIOUS_BEGINNER -> 
                summary.append("you're taking important first steps in managing your finances. " +
                    "It's normal to feel uncertain, and we're here to help guide you with " +
                    "practical, manageable steps.");
            
            case STABLE_BUILDER -> 
                summary.append("you demonstrate solid financial habits and discipline. " +
                    "You're on a good path, and there are opportunities to optimize " +
                    "your strategy for even better results.");
            
            case MODERATE_MANAGER -> 
                summary.append("you show a balanced approach to finances with room for improvement. " +
                    "With some targeted adjustments, you can strengthen your financial position.");
        }
        
        return summary.toString();
    }
    
    /**
     * Explain each score in user-friendly terms.
     */
    private List<ScoreExplanation> explainScores(UserBehaviorProfile profile) {
        List<ScoreExplanation> explanations = new ArrayList<>();
        
        // Overspending Score
        BigDecimal overspending = profile.getOverspendingScore();
        explanations.add(ScoreExplanation.builder()
            .scoreName("Spending Patterns")
            .scoreValue(overspending)
            .level(getScoreLevel(overspending))
            .explanation(explainOverspendingScore(overspending))
            .icon(overspending.doubleValue() >= 60 ? "⚠️" : "✓")
            .build());
        
        // Debt Risk Score
        BigDecimal debtRisk = profile.getDebtRiskScore();
        explanations.add(ScoreExplanation.builder()
            .scoreName("Debt Risk")
            .scoreValue(debtRisk)
            .level(getScoreLevel(debtRisk))
            .explanation(explainDebtRiskScore(debtRisk))
            .icon(debtRisk.doubleValue() >= 60 ? "⚠️" : "✓")
            .build());
        
        // Savings Capacity (inverse - higher is better)
        BigDecimal savings = profile.getSavingsCapacityScore();
        String savingsLevel = savings.doubleValue() >= 60 ? "GOOD" : 
                             savings.doubleValue() >= 40 ? "MODERATE" : "NEEDS_ATTENTION";
        explanations.add(ScoreExplanation.builder()
            .scoreName("Savings Ability")
            .scoreValue(savings)
            .level(savingsLevel)
            .explanation(explainSavingsCapacity(savings))
            .icon(savings.doubleValue() >= 50 ? "✓" : "⚠️")
            .build());
        
        // Anxiety Index
        BigDecimal anxiety = profile.getFinancialAnxietyIndex();
        explanations.add(ScoreExplanation.builder()
            .scoreName("Financial Wellness")
            .scoreValue(anxiety)
            .level(getAnxietyLevel(anxiety))
            .explanation(explainAnxietyIndex(anxiety))
            .icon(anxiety.doubleValue() >= 60 ? "💡" : "😊")
            .build());
        
        return explanations;
    }
    
    private String explainOverspendingScore(BigDecimal score) {
        double val = score.doubleValue();
        if (val >= 70) {
            return "You may be spending more than planned frequently. Consider setting up spending alerts.";
        } else if (val >= 50) {
            return "You occasionally overspend. A simple budget review could help.";
        } else {
            return "You manage your spending well. Keep up the good habits!";
        }
    }
    
    private String explainDebtRiskScore(BigDecimal score) {
        double val = score.doubleValue();
        if (val >= 70) {
            return "Your debt situation needs attention. Let's create a plan to reduce this risk.";
        } else if (val >= 50) {
            return "You have some debt exposure. Monitoring this can prevent future issues.";
        } else {
            return "Your debt risk is low. You're managing obligations well.";
        }
    }
    
    private String explainSavingsCapacity(BigDecimal score) {
        double val = score.doubleValue();
        if (val >= 70) {
            return "Great savings habits! You're building a solid financial cushion.";
        } else if (val >= 40) {
            return "There's room to boost your savings. Even small increases add up.";
        } else {
            return "Building savings is challenging right now. Let's find small wins.";
        }
    }
    
    private String explainAnxietyIndex(BigDecimal score) {
        double val = score.doubleValue();
        if (val >= 70) {
            return "Finances may be causing stress. We're here to help you feel more confident.";
        } else if (val >= 50) {
            return "Some financial concerns are normal. Knowledge and planning help reduce worry.";
        } else {
            return "You seem confident about your finances. That's a great foundation!";
        }
    }
    
    private String getScoreLevel(BigDecimal score) {
        double val = score.doubleValue();
        if (val >= 70) return "HIGH_RISK";
        if (val >= 50) return "MODERATE_RISK";
        return "LOW_RISK";
    }
    
    private String getAnxietyLevel(BigDecimal score) {
        double val = score.doubleValue();
        if (val >= 70) return "HIGH_STRESS";
        if (val >= 50) return "MODERATE_STRESS";
        return "LOW_STRESS";
    }
    
    /**
     * Explain why the user was classified into their segment.
     */
    private String explainSegment(BehavioralSegment segment, UserBehaviorProfile profile) {
        return switch (segment) {
            case FINANCIALLY_AT_RISK -> 
                String.format("You're classified as %s because your debt risk score (%.0f) " +
                    "is elevated and your savings capacity (%.0f) is limited. " +
                    "This combination suggests focusing on stabilization first.",
                    segment.getDisplayName(),
                    profile.getDebtRiskScore(),
                    profile.getSavingsCapacityScore());
            
            case IMPULSE_SPENDER -> 
                String.format("You're classified as %s because your responses indicate " +
                    "frequent unplanned purchases (overspending score: %.0f). " +
                    "This is common and very manageable with the right tools.",
                    segment.getDisplayName(),
                    profile.getOverspendingScore());
            
            case HIGH_EARNER_LOW_CONTROL -> 
                String.format("You're classified as %s because despite good earning/saving capacity (%.0f), " +
                    "your spending patterns (%.0f) suggest money flows out quickly. " +
                    "You have great potential to build wealth with better controls.",
                    segment.getDisplayName(),
                    profile.getSavingsCapacityScore(),
                    profile.getOverspendingScore());
            
            case FINANCIALLY_ANXIOUS_BEGINNER -> 
                String.format("You're classified as %s because your financial anxiety (%.0f) " +
                    "is elevated while experience is still developing. " +
                    "This is a perfect time to build confidence through education.",
                    segment.getDisplayName(),
                    profile.getFinancialAnxietyIndex());
            
            case STABLE_BUILDER -> 
                String.format("You're classified as %s because you show consistent positive behaviors: " +
                    "good savings (%.0f), controlled spending (%.0f), " +
                    "and low debt risk (%.0f). You're doing great!",
                    segment.getDisplayName(),
                    profile.getSavingsCapacityScore(),
                    100 - profile.getOverspendingScore().doubleValue(),
                    100 - profile.getDebtRiskScore().doubleValue());
            
            case MODERATE_MANAGER -> 
                String.format("You're classified as %s because your scores are balanced " +
                    "across categories. This is a solid foundation with room for targeted improvements.",
                    segment.getDisplayName());
        };
    }
    
    /**
     * Extract top contributing factors from the profile.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractTopFactors(UserBehaviorProfile profile) {
        if (profile.getExplanationFactors() == null) {
            return List.of("Survey responses analyzed", "Behavioral patterns identified");
        }
        
        Object factors = profile.getExplanationFactors().get("topFactors");
        if (factors instanceof List) {
            return (List<String>) factors;
        }
        
        return List.of("Your responses have been analyzed to create this profile");
    }
    
    /**
     * Generate personalized recommendations based on segment and scores.
     */
    private List<RecommendedAction> generateRecommendations(UserBehaviorProfile profile) {
        List<RecommendedAction> actions = new ArrayList<>();
        BehavioralSegment segment = profile.getSegment();
        
        // Segment-specific primary recommendation
        switch (segment) {
            case FINANCIALLY_AT_RISK -> {
                actions.add(RecommendedAction.builder()
                    .priority(1)
                    .category("DEBT")
                    .title("Create a Debt Payoff Plan")
                    .description("List all your debts and prioritize by interest rate. Even small extra payments help.")
                    .expectedImpact("HIGH")
                    .build());
                actions.add(RecommendedAction.builder()
                    .priority(2)
                    .category("SAVING")
                    .title("Start a Mini Emergency Fund")
                    .description("Aim to save just $500-$1000 first. This prevents new debt from emergencies.")
                    .expectedImpact("HIGH")
                    .build());
            }
            case IMPULSE_SPENDER -> {
                actions.add(RecommendedAction.builder()
                    .priority(1)
                    .category("SPENDING")
                    .title("Enable Spending Notifications")
                    .description("Real-time alerts help you pause before impulse purchases.")
                    .expectedImpact("HIGH")
                    .build());
                actions.add(RecommendedAction.builder()
                    .priority(2)
                    .category("SPENDING")
                    .title("Try the 24-Hour Rule")
                    .description("Wait 24 hours before any non-essential purchase over $50.")
                    .expectedImpact("MEDIUM")
                    .build());
            }
            case HIGH_EARNER_LOW_CONTROL -> {
                actions.add(RecommendedAction.builder()
                    .priority(1)
                    .category("SAVING")
                    .title("Automate Your Savings")
                    .description("Set up automatic transfers on payday - pay yourself first!")
                    .expectedImpact("HIGH")
                    .build());
                actions.add(RecommendedAction.builder()
                    .priority(2)
                    .category("SPENDING")
                    .title("Set Category Budgets")
                    .description("Create spending limits for discretionary categories like dining and entertainment.")
                    .expectedImpact("MEDIUM")
                    .build());
            }
            case FINANCIALLY_ANXIOUS_BEGINNER -> {
                actions.add(RecommendedAction.builder()
                    .priority(1)
                    .category("LEARNING")
                    .title("Complete Financial Basics Course")
                    .description("Knowledge reduces anxiety. Start with our beginner-friendly modules.")
                    .expectedImpact("HIGH")
                    .build());
                actions.add(RecommendedAction.builder()
                    .priority(2)
                    .category("PLANNING")
                    .title("Set One Simple Goal")
                    .description("Pick one achievable goal (like saving $100) and track your progress.")
                    .expectedImpact("MEDIUM")
                    .build());
            }
            case STABLE_BUILDER -> {
                actions.add(RecommendedAction.builder()
                    .priority(1)
                    .category("OPTIMIZATION")
                    .title("Optimize Your Returns")
                    .description("Consider moving savings to higher-yield accounts or investments.")
                    .expectedImpact("MEDIUM")
                    .build());
                actions.add(RecommendedAction.builder()
                    .priority(2)
                    .category("PLANNING")
                    .title("Review Long-Term Goals")
                    .description("You're in a great position to think about retirement and major life goals.")
                    .expectedImpact("MEDIUM")
                    .build());
            }
            case MODERATE_MANAGER -> {
                actions.add(RecommendedAction.builder()
                    .priority(1)
                    .category("PLANNING")
                    .title("Create a Spending Review Routine")
                    .description("Schedule weekly 15-minute check-ins on your spending.")
                    .expectedImpact("MEDIUM")
                    .build());
            }
        }
        
        // Add universal recommendation if space
        if (actions.size() < 3) {
            actions.add(RecommendedAction.builder()
                .priority(3)
                .category("TRACKING")
                .title("Log Your Transactions")
                .description("The more we track, the better insights we can provide.")
                .expectedImpact("LOW")
                .build());
        }
        
        return actions;
    }
    
    // ===== Result DTOs =====
    
    @Data
    @Builder
    public static class ProfileExplanation {
        private String overallSummary;
        private String riskLevel;
        private List<ScoreExplanation> scoreInsights;
        private String segmentRationale;
        private List<String> topFactors;
        private List<RecommendedAction> recommendations;
    }
    
    @Data
    @Builder
    public static class ScoreExplanation {
        private String scoreName;
        private BigDecimal scoreValue;
        private String level;
        private String explanation;
        private String icon;
    }
    
    @Data
    @Builder
    public static class RecommendedAction {
        private int priority;
        private String category;
        private String title;
        private String description;
        private String expectedImpact;
    }
}
