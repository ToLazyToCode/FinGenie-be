package fingenie.com.fingenie.ai.service;

import fingenie.com.fingenie.ai.runtime.AIRuntimeService;
import fingenie.com.fingenie.entity.UserAISpendingProfile;
import fingenie.com.fingenie.entity.UserSpendingRoutine;
import fingenie.com.fingenie.repository.UserAISpendingProfileRepository;
import fingenie.com.fingenie.repository.UserSpendingRoutineRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIPredictionEngineService {

    private final AIRuntimeService aiRuntimeService;
    private final UserAISpendingProfileRepository profileRepository;
    private final UserSpendingRoutineRepository routineRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate daily spending prediction for a user
     */
    public Map<String, Object> generateDailyPrediction(Long userId) {
        try {
            // Get user profile
            UserAISpendingProfile profile = profileRepository.findByUserId(userId).orElse(null);
            
            // Get active routines
            List<UserSpendingRoutine> routines = routineRepository.findByUserIdAndIsActiveTrue(userId);
            
            // Build prediction context
            Map<String, Object> context = buildPredictionContext(profile, routines);
            
            // Generate AI prediction if confidence is high enough
            if (profile != null && profile.getConfidenceScore().compareTo(new BigDecimal("0.30")) > 0) {
                return generateAIBasedPrediction(userId, profile, context);
            } else {
                // Fall back to rule-based prediction
                return generateRuleBasedPrediction(profile, routines, context);
            }
            
        } catch (Exception e) {
            log.error("Error generating prediction for userId: {}", userId, e);
            return createFallbackPrediction();
        }
    }

    /**
     * Generate category-specific prediction
     */
    public Map<String, Object> generateCategoryPrediction(Long userId, String category) {
        try {
            UserAISpendingProfile profile = profileRepository.findByUserId(userId).orElse(null);
            List<UserSpendingRoutine> routines = routineRepository
                .findByUserIdAndRoutineTypeAndIsActiveTrue(userId, "DAILY");
            
            // Filter routines by category if available
            if (profile != null && profile.getCategoryProbabilityMap() != null) {
                Map<String, BigDecimal> categoryProbs = parseCategoryProbabilities(profile.getCategoryProbabilityMap());
                BigDecimal categoryProb = categoryProbs.getOrDefault(category.toUpperCase(), BigDecimal.ZERO);
                
                return Map.of(
                    "category", category,
                    "predictedAmount", calculateCategoryAmount(categoryProb, profile.getBaselineDailySpending()),
                    "confidence", categoryProb.multiply(profile.getConfidenceScore()),
                    "source", "HYBRID",
                    "reasoning", String.format("Based on %s category probability and user patterns", category.toLowerCase())
                );
            }
            
            return createFallbackCategoryPrediction(category);
            
        } catch (Exception e) {
            log.error("Error generating category prediction for userId: {}, category: {}", userId, category, e);
            return createFallbackCategoryPrediction(category);
        }
    }

    private Map<String, Object> buildPredictionContext(UserAISpendingProfile profile, 
                                                   List<UserSpendingRoutine> routines) {
        Map<String, Object> context = new HashMap<>();
        
        if (profile != null) {
            context.put("baselineDailySpending", profile.getBaselineDailySpending());
            context.put("weekdayVsWeekendPattern", profile.getWeekdayVsWeekendPattern());
            context.put("confidenceScore", profile.getConfidenceScore());
            context.put("volatilityScore", profile.getVolatilityScore());
        }
        
        context.put("activeRoutines", routines.size());
        context.put("currentDay", LocalDateTime.now().getDayOfWeek().toString());
        context.put("currentTime", LocalTime.now().toString());
        
        return context;
    }

    private Map<String, Object> generateAIBasedPrediction(Long userId, UserAISpendingProfile profile, 
                                                        Map<String, Object> context) {
        try {
            // Use AI runtime for prediction
            var aiResponse = aiRuntimeService.generatePrediction(userId, "DAILY_SPENDING");
            
            // Parse AI response
            Map<String, Object> aiPrediction = parseAIResponse(aiResponse.getText());
            
            // Enhance with profile data
            aiPrediction.put("confidence", profile.getConfidenceScore());
            aiPrediction.put("source", "AI");
            aiPrediction.put("userId", userId);
            aiPrediction.put("timestamp", LocalDateTime.now());
            
            return aiPrediction;
            
        } catch (Exception e) {
            log.error("Error in AI-based prediction, falling back to rules", e);
            return generateRuleBasedPrediction(profile, null, context);
        }
    }

    private Map<String, Object> generateRuleBasedPrediction(UserAISpendingProfile profile, 
                                                         List<UserSpendingRoutine> routines,
                                                         Map<String, Object> context) {
        BigDecimal baseline = profile != null ? profile.getBaselineDailySpending() : new BigDecimal("50.00");
        
        // Adjust for weekday/weekend
        DayOfWeek dayOfWeek = LocalDateTime.now().getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        
        BigDecimal adjustment = isWeekend && profile != null ? 
            profile.getWeekdayVsWeekendPattern() : BigDecimal.ONE;
        
        BigDecimal predictedAmount = baseline.multiply(adjustment);
        
        // Add some randomness for realism
        Random random = new Random();
        double variance = 0.2; // 20% variance
        double factor = 1 + (random.nextGaussian() * variance);
        predictedAmount = predictedAmount.multiply(new BigDecimal(factor))
            .setScale(2, RoundingMode.HALF_UP);
        
        return Map.of(
            "predictedAmount", predictedAmount,
            "category", predictMostLikelyCategory(profile),
            "confidence", profile != null ? profile.getConfidenceScore() : new BigDecimal("0.30"),
            "source", "RULE_BASED",
            "reasoning", String.format("Based on baseline $%.2f adjusted for %s", 
                baseline, isWeekend ? "weekend" : "weekday"),
            "timestamp", LocalDateTime.now()
        );
    }

    private String predictMostLikelyCategory(UserAISpendingProfile profile) {
        if (profile == null || profile.getCategoryProbabilityMap() == null) {
            return "FOOD"; // Default category
        }
        
        Map<String, BigDecimal> categoryProbs = parseCategoryProbabilities(profile.getCategoryProbabilityMap());
        
        // Find category with highest probability
        return categoryProbs.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("FOOD");
    }

    private Map<String, BigDecimal> parseCategoryProbabilities(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return getDefaultCategoryProbabilities();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            
            Map<String, BigDecimal> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    result.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
                }
            }
            
            return result.isEmpty() ? getDefaultCategoryProbabilities() : result;
            
        } catch (Exception e) {
            log.error("Error parsing category probabilities", e);
            return getDefaultCategoryProbabilities();
        }
    }

    private Map<String, BigDecimal> getDefaultCategoryProbabilities() {
        Map<String, BigDecimal> defaults = new HashMap<>();
        defaults.put("FOOD", new BigDecimal("0.30"));
        defaults.put("TRANSPORT", new BigDecimal("0.20"));
        defaults.put("ENTERTAINMENT", new BigDecimal("0.15"));
        defaults.put("SHOPPING", new BigDecimal("0.15"));
        defaults.put("OTHER", new BigDecimal("0.20"));
        return defaults;
    }

    private BigDecimal calculateCategoryAmount(BigDecimal categoryProb, BigDecimal baseline) {
        return baseline.multiply(categoryProb.multiply(new BigDecimal("2"))) // Some categories cost more
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> parseAIResponse(String aiResponse) {
        try {
            // Try to parse as JSON first
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(aiResponse, Map.class);
            return parsed;
        } catch (Exception e) {
            // If not JSON, create a simple response
            return Map.of(
                "predictedAmount", new BigDecimal("50.00"),
                "category", "FOOD",
                "reasoning", aiResponse.length() > 100 ? aiResponse.substring(0, 100) + "..." : aiResponse
            );
        }
    }

    private Map<String, Object> createFallbackPrediction() {
        return Map.of(
            "predictedAmount", new BigDecimal("50.00"),
            "category", "FOOD",
            "confidence", new BigDecimal("0.20"),
            "source", "FALLBACK",
            "reasoning", "Default prediction due to system error",
            "timestamp", LocalDateTime.now()
        );
    }

    private Map<String, Object> createFallbackCategoryPrediction(String category) {
        return Map.of(
            "category", category,
            "predictedAmount", new BigDecimal("25.00"),
            "confidence", new BigDecimal("0.25"),
            "source", "FALLBACK",
            "reasoning", "Default category prediction"
        );
    }
}
