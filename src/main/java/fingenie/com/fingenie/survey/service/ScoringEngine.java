package fingenie.com.fingenie.survey.service;

import fingenie.com.fingenie.survey.enums.BehavioralSegment;
import fingenie.com.fingenie.survey.enums.SurveyAnswers.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Scoring Engine for Behavioral Survey.
 * 
 * Computes 4 deterministic scores (0-100 scale):
 * - overspending_score: Risk of overspending behavior
 * - debt_risk_score: Risk of debt accumulation
 * - savings_capacity_score: Ability to save (higher = better)
 * - financial_anxiety_index: Level of financial stress
 * 
 * Also classifies users into one of 6 behavioral segments.
 */
@Service
@Slf4j
public class ScoringEngine {
    
    // Question weights for each score calculation
    private static final Map<String, BigDecimal> OVERSPENDING_WEIGHTS = Map.of(
        "B1", new BigDecimal("1.5"),   // UnplannedPurchaseFrequency
        "B2", new BigDecimal("1.3"),   // EmotionalSpendingImpact
        "B3", new BigDecimal("1.2"),   // LargePurchaseApproach
        "B4", new BigDecimal("1.0"),   // BudgetExceedFrequency
        "A3", new BigDecimal("0.8")    // FixedExpenseRatio
    );
    
    private static final Map<String, BigDecimal> DEBT_RISK_WEIGHTS = Map.of(
        "C1", new BigDecimal("1.5"),   // DebtSituation
        "C2", new BigDecimal("1.2"),   // BillWorryFrequency
        "C3", new BigDecimal("1.3"),   // BorrowForExpenses
        "A2", new BigDecimal("1.0")    // IncomeStability
    );
    
    private static final Map<String, BigDecimal> SAVINGS_CAPACITY_WEIGHTS = Map.of(
        "D1", new BigDecimal("1.5"),   // EmergencyFundStatus
        "D2", new BigDecimal("1.3"),   // SavingsConsistency
        "A1", new BigDecimal("1.0"),   // IncomeSource
        "A3", new BigDecimal("0.8")    // FixedExpenseRatio (inverse contribution)
    );
    
    private static final Map<String, BigDecimal> ANXIETY_WEIGHTS = Map.of(
        "C2", new BigDecimal("1.5"),   // BillWorryFrequency
        "D4", new BigDecimal("1.3"),   // GoalConfidence (inverse)
        "E1", new BigDecimal("1.0"),   // ExpenseTrackingLevel (inverse)
        "E3", new BigDecimal("0.8")    // FinancialKnowledgeLevel (inverse)
    );
    
    /**
     * Calculate all scores from survey responses.
     * 
     * @param answers Map of question_code -> answer_code (enum name)
     * @return ScoringResult containing all scores and segment
     */
    public ScoringResult calculateScores(Map<String, String> answers) {
        log.debug("Calculating scores for {} answers", answers.size());
        
        Map<String, Integer> scoreValues = resolveScoreValues(answers);
        
        BigDecimal overspendingScore = calculateOverspendingScore(scoreValues);
        BigDecimal debtRiskScore = calculateDebtRiskScore(scoreValues);
        BigDecimal savingsCapacityScore = calculateSavingsCapacityScore(scoreValues);
        BigDecimal anxietyIndex = calculateAnxietyIndex(scoreValues);
        
        BehavioralSegment segment = classifySegment(
            overspendingScore, debtRiskScore, savingsCapacityScore, anxietyIndex);
        
        List<String> topFactors = identifyTopFactors(scoreValues, segment);
        
        return ScoringResult.builder()
            .overspendingScore(overspendingScore)
            .debtRiskScore(debtRiskScore)
            .savingsCapacityScore(savingsCapacityScore)
            .financialAnxietyIndex(anxietyIndex)
            .segment(segment)
            .segmentConfidence(calculateSegmentConfidence(
                overspendingScore, debtRiskScore, savingsCapacityScore, anxietyIndex, segment))
            .topFactors(topFactors)
            .featureVector(buildFeatureVector(scoreValues))
            .build();
    }
    
    /**
     * Resolve answer codes to their numeric score values.
     */
    private Map<String, Integer> resolveScoreValues(Map<String, String> answers) {
        Map<String, Integer> scoreValues = new HashMap<>();
        
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String questionCode = entry.getKey();
            String answerCode = entry.getValue();
            
            Integer scoreValue = getScoreForAnswer(questionCode, answerCode);
            if (scoreValue != null) {
                scoreValues.put(questionCode, scoreValue);
            }
        }
        
        return scoreValues;
    }
    
    /**
     * Get score value for a specific answer.
     */
    private Integer getScoreForAnswer(String questionCode, String answerCode) {
        try {
            return switch (questionCode) {
                case "A1" -> IncomeSource.valueOf(answerCode).getScoreValue();
                case "A2" -> IncomeStability.valueOf(answerCode).getScoreValue();
                case "A3" -> FixedExpenseRatio.valueOf(answerCode).getScoreValue();
                case "B1" -> UnplannedPurchaseFrequency.valueOf(answerCode).getScoreValue();
                case "B2" -> EmotionalSpendingImpact.valueOf(answerCode).getScoreValue();
                case "B3" -> LargePurchaseApproach.valueOf(answerCode).getScoreValue();
                case "B4" -> BudgetExceedFrequency.valueOf(answerCode).getScoreValue();
                case "C1" -> DebtSituation.valueOf(answerCode).getScoreValue();
                case "C2" -> BillWorryFrequency.valueOf(answerCode).getScoreValue();
                case "C3" -> BorrowForExpenses.valueOf(answerCode).getScoreValue();
                case "D1" -> EmergencyFundStatus.valueOf(answerCode).getScoreValue();
                case "D2" -> SavingsConsistency.valueOf(answerCode).getScoreValue();
                case "D3" -> PrimaryFinancialGoal.valueOf(answerCode).getScoreValue();
                case "D4" -> GoalConfidence.valueOf(answerCode).getScoreValue();
                case "E1" -> ExpenseTrackingLevel.valueOf(answerCode).getScoreValue();
                case "E2" -> FinancialReviewFrequency.valueOf(answerCode).getScoreValue();
                case "E3" -> FinancialKnowledgeLevel.valueOf(answerCode).getScoreValue();
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            log.warn("Unknown answer code {} for question {}", answerCode, questionCode);
            return null;
        }
    }
    
    /**
     * Calculate overspending score (0-100).
     * Higher score = higher overspending risk.
     */
    private BigDecimal calculateOverspendingScore(Map<String, Integer> scoreValues) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : OVERSPENDING_WEIGHTS.entrySet()) {
            Integer score = scoreValues.get(entry.getKey());
            if (score != null) {
                weightedSum = weightedSum.add(
                    entry.getValue().multiply(new BigDecimal(score)));
                totalWeight = totalWeight.add(entry.getValue());
            }
        }
        
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate debt risk score (0-100).
     * Higher score = higher debt risk.
     */
    private BigDecimal calculateDebtRiskScore(Map<String, Integer> scoreValues) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : DEBT_RISK_WEIGHTS.entrySet()) {
            Integer score = scoreValues.get(entry.getKey());
            if (score != null) {
                // A2 (IncomeStability): Lower stability = higher debt risk
                int adjustedScore = entry.getKey().equals("A2") ? 100 - score : score;
                weightedSum = weightedSum.add(
                    entry.getValue().multiply(new BigDecimal(adjustedScore)));
                totalWeight = totalWeight.add(entry.getValue());
            }
        }
        
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate savings capacity score (0-100).
     * Higher score = better savings capacity.
     */
    private BigDecimal calculateSavingsCapacityScore(Map<String, Integer> scoreValues) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : SAVINGS_CAPACITY_WEIGHTS.entrySet()) {
            Integer score = scoreValues.get(entry.getKey());
            if (score != null) {
                // A3 (FixedExpenseRatio): Lower ratio = better savings capacity
                int adjustedScore = entry.getKey().equals("A3") ? 100 - score : score;
                weightedSum = weightedSum.add(
                    entry.getValue().multiply(new BigDecimal(adjustedScore)));
                totalWeight = totalWeight.add(entry.getValue());
            }
        }
        
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate financial anxiety index (0-100).
     * Higher score = higher anxiety.
     */
    private BigDecimal calculateAnxietyIndex(Map<String, Integer> scoreValues) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : ANXIETY_WEIGHTS.entrySet()) {
            Integer score = scoreValues.get(entry.getKey());
            if (score != null) {
                // D4, E1, E3: Higher values = lower anxiety (inverse)
                int adjustedScore = switch (entry.getKey()) {
                    case "D4", "E1", "E3" -> 100 - score;
                    default -> score;
                };
                weightedSum = weightedSum.add(
                    entry.getValue().multiply(new BigDecimal(adjustedScore)));
                totalWeight = totalWeight.add(entry.getValue());
            }
        }
        
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Classify user into a behavioral segment based on scores.
     */
    private BehavioralSegment classifySegment(
            BigDecimal overspending, BigDecimal debtRisk, 
            BigDecimal savingsCapacity, BigDecimal anxiety) {
        
        double os = overspending.doubleValue();
        double dr = debtRisk.doubleValue();
        double sc = savingsCapacity.doubleValue();
        double ax = anxiety.doubleValue();
        
        // Priority-based classification (check highest priority segments first)
        
        // 1. FINANCIALLY_AT_RISK (Priority 1)
        if (dr >= 70 && sc < 30) {
            return BehavioralSegment.FINANCIALLY_AT_RISK;
        }
        
        // 2. IMPULSE_SPENDER (Priority 2)
        if (os >= 60 && sc < 50) {
            return BehavioralSegment.IMPULSE_SPENDER;
        }
        
        // 3. HIGH_EARNER_LOW_CONTROL (Priority 3)
        if (sc >= 50 && os >= 50) {
            return BehavioralSegment.HIGH_EARNER_LOW_CONTROL;
        }
        
        // 4. FINANCIALLY_ANXIOUS_BEGINNER (Priority 4)
        if (ax >= 60 && sc < 40) {
            return BehavioralSegment.FINANCIALLY_ANXIOUS_BEGINNER;
        }
        
        // 5. STABLE_BUILDER (Priority 5)
        if (sc >= 60 && os < 40 && dr < 40) {
            return BehavioralSegment.STABLE_BUILDER;
        }
        
        // 6. Default: MODERATE_MANAGER
        return BehavioralSegment.MODERATE_MANAGER;
    }
    
    /**
     * Calculate confidence level for segment classification.
     */
    private BigDecimal calculateSegmentConfidence(
            BigDecimal overspending, BigDecimal debtRisk,
            BigDecimal savingsCapacity, BigDecimal anxiety,
            BehavioralSegment segment) {
        
        double os = overspending.doubleValue();
        double dr = debtRisk.doubleValue();
        double sc = savingsCapacity.doubleValue();
        double ax = anxiety.doubleValue();
        
        // Calculate how strongly the user fits their segment
        double confidence = switch (segment) {
            case FINANCIALLY_AT_RISK -> Math.min(1.0, (dr / 100 + (100 - sc) / 100) / 2 + 0.2);
            case IMPULSE_SPENDER -> Math.min(1.0, os / 100 + 0.2);
            case HIGH_EARNER_LOW_CONTROL -> Math.min(1.0, (sc / 100 + os / 100) / 2 + 0.1);
            case FINANCIALLY_ANXIOUS_BEGINNER -> Math.min(1.0, ax / 100 + 0.1);
            case STABLE_BUILDER -> Math.min(1.0, (sc / 100 + (100 - os) / 100 + (100 - dr) / 100) / 3 + 0.1);
            case MODERATE_MANAGER -> 0.7; // Default segment has moderate confidence
        };
        
        return new BigDecimal(confidence).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Identify top 3 contributing factors for the user's profile.
     */
    private List<String> identifyTopFactors(Map<String, Integer> scoreValues, BehavioralSegment segment) {
        List<String> factors = new ArrayList<>();
        
        // Identify concerning factors based on score values
        Map<String, String> factorDescriptions = new LinkedHashMap<>();
        
        Integer b1 = scoreValues.get("B1");
        if (b1 != null && b1 >= 60) {
            factorDescriptions.put("B1", "Frequent unplanned purchases");
        }
        
        Integer b2 = scoreValues.get("B2");
        if (b2 != null && b2 >= 70) {
            factorDescriptions.put("B2", "High emotional spending impact");
        }
        
        Integer c1 = scoreValues.get("C1");
        if (c1 != null && c1 >= 60) {
            factorDescriptions.put("C1", "Existing debt situation");
        }
        
        Integer c2 = scoreValues.get("C2");
        if (c2 != null && c2 >= 60) {
            factorDescriptions.put("C2", "Frequent worry about bills");
        }
        
        Integer d1 = scoreValues.get("D1");
        if (d1 != null && d1 < 40) {
            factorDescriptions.put("D1", "Limited emergency fund");
        }
        
        Integer d2 = scoreValues.get("D2");
        if (d2 != null && d2 < 40) {
            factorDescriptions.put("D2", "Inconsistent savings habits");
        }
        
        Integer e1 = scoreValues.get("E1");
        if (e1 != null && e1 < 40) {
            factorDescriptions.put("E1", "Limited expense tracking");
        }
        
        // Return top 3 factors
        int count = 0;
        for (String description : factorDescriptions.values()) {
            if (count >= 3) break;
            factors.add(description);
            count++;
        }
        
        // If no concerning factors, add positive observations
        if (factors.isEmpty()) {
            factors.add("Balanced financial behaviors");
        }
        
        return factors;
    }
    
    /**
     * Build feature vector for AI service.
     */
    private Map<String, Double> buildFeatureVector(Map<String, Integer> scoreValues) {
        Map<String, Double> features = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : scoreValues.entrySet()) {
            // Normalize to 0-1 range for ML
            features.put("survey_" + entry.getKey().toLowerCase(), 
                entry.getValue() / 100.0);
        }
        
        return features;
    }
    
    /**
     * Result object containing all calculated scores.
     */
    @lombok.Builder
    @lombok.Getter
    public static class ScoringResult {
        private BigDecimal overspendingScore;
        private BigDecimal debtRiskScore;
        private BigDecimal savingsCapacityScore;
        private BigDecimal financialAnxietyIndex;
        private BehavioralSegment segment;
        private BigDecimal segmentConfidence;
        private List<String> topFactors;
        private Map<String, Double> featureVector;
    }
}
