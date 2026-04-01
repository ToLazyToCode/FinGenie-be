package fingenie.com.fingenie.ai.service;

import fingenie.com.fingenie.ai.runtime.AIRuntimeService;
import fingenie.com.fingenie.entity.PetProfile;
import fingenie.com.fingenie.entity.UserAISpendingProfile;
import fingenie.com.fingenie.dto.XPLogRequest;
import fingenie.com.fingenie.service.XPLogService;
import fingenie.com.fingenie.repository.PetProfileRepository;
import fingenie.com.fingenie.repository.UserAISpendingProfileRepository;
import fingenie.com.fingenie.service.GamificationService;
import fingenie.com.fingenie.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIGamificationIntegrationService {

    private final GamificationService gamificationService;
    private final PetService petService;
    private final XPLogService xpLogService;
    private final AIRuntimeService aiRuntimeService;
    private final UserAISpendingProfileRepository profileRepository;
    private final PetProfileRepository petProfileRepository;

    /**
     * Award XP for prediction acceptance
     */
    @Transactional
    public void awardXPPredictionAccepted(Long userId, Long predictionId) {
        try {
            // Base XP for prediction
            int baseXP = 10;
            
            // Bonus XP based on prediction accuracy
            UserAISpendingProfile profile = profileRepository.findByUserId(userId).orElse(null);
            if (profile != null) {
                // More XP for higher confidence predictions that were accepted
                BigDecimal confidenceBonus = profile.getConfidenceScore().multiply(new BigDecimal("20"));
                baseXP += confidenceBonus.intValue();
            }
            
            // Award XP
            gamificationService.addXp(userId, baseXP);
            
            // Create XP log entry
            XPLogRequest xpRequest = new XPLogRequest();
            xpRequest.setAccountId(userId);
            xpRequest.setXpAmount(baseXP);
            xpRequest.setSourceType("AI_PREDICTION_ACCEPTED");
            xpRequest.setDescription("AI prediction accepted and used");
            xpLogService.create(xpRequest);
            
            // Update pet mood
            updatePetMoodFromAIEvent(userId, "PREDICTION_ACCEPTED", baseXP);
            
            log.info("Awarded {} XP to user {} for accepting AI prediction", baseXP, userId);
            
        } catch (Exception e) {
            log.error("Error awarding XP for prediction acceptance for userId: {}", userId, e);
        }
    }

    /**
     * Award XP for streak maintained with AI help
     */
    @Transactional
    public void awardXPAIStreakHelp(Long userId, int streakDays) {
        try {
            // XP scales with streak length
            int streakXP = Math.min(5 * streakDays, 50); // Cap at 50 XP
            
            gamificationService.addXp(userId, streakXP);
            
            // Create XP log entry
            XPLogRequest xpRequest = new XPLogRequest();
            xpRequest.setAccountId(userId);
            xpRequest.setXpAmount(streakXP);
            xpRequest.setSourceType("AI_STREAK_MAINTENANCE");
            xpRequest.setDescription(String.format("Maintained %d day streak with AI assistance", streakDays));
            xpLogService.create(xpRequest);
            
            // Generate AI encouragement message
            var aiResponse = aiRuntimeService.generateGamificationMessage(userId, "STREAK_ACHIEVEMENT");
            
            // Update pet mood positively
            updatePetMoodFromAIEvent(userId, "STREAK_MAINTAINED", streakXP);
            
            log.info("Awarded {} XP to user {} for maintaining {} day streak with AI help", 
                streakXP, userId, streakDays);
            
        } catch (Exception e) {
            log.error("Error awarding XP for AI streak help for userId: {}", userId, e);
        }
    }

    /**
     * Award XP for savings milestone reached with AI predictions
     */
    @Transactional
    public void awardXPSavingsMilestone(Long userId, BigDecimal savedAmount, String milestoneType) {
        try {
            // XP based on amount saved
            int savingsXP = savedAmount.divide(new BigDecimal("10"), 0, RoundingMode.DOWN)
                .intValue();
            savingsXP = Math.min(savingsXP, 100); // Cap at 100 XP
            
            // Bonus for milestone type
            if ("WEEKLY".equals(milestoneType)) {
                savingsXP += 20;
            } else if ("MONTHLY".equals(milestoneType)) {
                savingsXP += 50;
            }
            
            gamificationService.addXp(userId, savingsXP);
            
            // Create XP log entry
            XPLogRequest xpRequest = new XPLogRequest();
            xpRequest.setAccountId(userId);
            xpRequest.setXpAmount(savingsXP);
            xpRequest.setSourceType("AI_SAVINGS_MILESTONE");
            xpRequest.setDescription(String.format("Reached %s savings milestone of $%.2f with AI guidance", 
                milestoneType.toLowerCase(), savedAmount));
            xpLogService.create(xpRequest);
            
            // Update pet mood
            updatePetMoodFromAIEvent(userId, "SAVINGS_MILESTONE", savingsXP);
            
            log.info("Awarded {} XP to user {} for reaching %s savings milestone of $%.2f", 
                savingsXP, userId, milestoneType, savedAmount);
            
        } catch (Exception e) {
            log.error("Error awarding XP for savings milestone for userId: {}", userId, e);
        }
    }

    /**
     * Update pet mood based on AI events
     */
    private void updatePetMoodFromAIEvent(Long userId, String eventType, int xpGained) {
        try {
            PetProfile pet = petService.getStateEntity(userId);
            if (pet == null) return;
            
            Integer currentMood = pet.getMood();
            int currentHappiness = pet.getHappiness() != null ? pet.getHappiness() : 50;
            
            // Calculate mood change based on event
            int moodChange = calculateMoodChange(eventType, xpGained);
            
            // Update happiness (capped at 0-100)
            int newHappiness = Math.max(0, Math.min(100, currentHappiness + moodChange));
            
            // Determine new mood based on happiness
            Integer newMood = determineMoodFromHappiness(newHappiness);
            
            // Update pet state
            pet.setHappiness(newHappiness);
            pet.setMood(newMood);
            petService.savePetProfile(pet);
            
            log.info("Updated pet mood for user {} from {} (happiness: {}) to {} (happiness: {}) due to {}", 
                userId, currentMood, currentHappiness, newMood, newHappiness, eventType);
            
        } catch (Exception e) {
            log.error("Error updating pet mood for userId: {}", userId, e);
        }
    }

    private int calculateMoodChange(String eventType, int xpGained) {
        // Base mood change from XP
        int moodChange = xpGained / 10; // 1 mood point per 10 XP
        
        // Adjust based on event type
        switch (eventType) {
            case "PREDICTION_ACCEPTED":
                moodChange += 2; // Positive reinforcement
                break;
            case "STREAK_MAINTAINED":
                moodChange += 5; // Big achievement
                break;
            case "SAVINGS_MILESTONE":
                moodChange += 3; // Financial discipline
                break;
            case "PREDICTION_REJECTED":
                moodChange -= 1; // Slight negative
                break;
            default:
                moodChange += 1; // Default small positive
        }
        
        return moodChange;
    }

    private Integer determineMoodFromHappiness(int happiness) {
        if (happiness >= 80) return 100; // EXCITED
        if (happiness >= 60) return 75;  // HAPPY
        if (happiness >= 40) return 50;  // CONTENT
        if (happiness >= 20) return 25;  // SAD
        return 0;  // DEPRESSED
    }

    /**
     * Generate AI-powered gamification message
     */
    public String generateGamificationMessage(Long userId, String context) {
        try {
            var aiResponse = aiRuntimeService.generateGamificationMessage(userId, context);
            return aiResponse.getText();
        } catch (Exception e) {
            log.error("Error generating gamification message for userId: {}, context: {}", userId, context, e);
            return getDefaultMessage(context);
        }
    }

    private String getDefaultMessage(String context) {
        switch (context) {
            case "STREAK_ACHIEVEMENT":
                return "Great job keeping up your streak! Every day counts!";
            case "SAVINGS_MILESTONE":
                return "Fantastic savings! Your future self will thank you!";
            case "LEVEL_UP":
                return "Level up! You're becoming a financial wizard!";
            default:
                return "Keep up the great work on your financial journey!";
        }
    }

    /**
     * Process AI learning milestone (when confidence improves)
     */
    @Transactional
    public void processAILearningMilestone(Long userId, BigDecimal oldConfidence, BigDecimal newConfidence) {
        try {
            BigDecimal improvement = newConfidence.subtract(oldConfidence);
            
            // Award XP for significant improvements
            if (improvement.compareTo(new BigDecimal("0.10")) >= 0) {
                int milestoneXP = improvement.multiply(new BigDecimal("100")).intValue();
                
                gamificationService.addXp(userId, milestoneXP);
                
                // Create XP log entry
                XPLogRequest xpRequest = new XPLogRequest();
                xpRequest.setAccountId(userId);
                xpRequest.setXpAmount(milestoneXP);
                xpRequest.setSourceType("AI_LEARNING_MILESTONE");
                xpRequest.setDescription(String.format("AI model improved by %.1f%% in understanding your habits", 
                    improvement.multiply(new BigDecimal("100"))));
                xpLogService.create(xpRequest);
                
                updatePetMoodFromAIEvent(userId, "AI_IMPROVED", milestoneXP);
                
                log.info("Awarded {} XP to user {} for AI learning milestone (confidence: {} -> {})", 
                    milestoneXP, userId, oldConfidence, newConfidence);
            }
            
        } catch (Exception e) {
            log.error("Error processing AI learning milestone for userId: {}", userId, e);
        }
    }

    /**
     * Award XP for providing feedback (helps AI learn)
     */
    @Transactional
    public void awardXPFeedbackProvided(Long userId, String feedbackType) {
        try {
            int feedbackXP = 5; // Base XP for any feedback
            
            // Extra XP for detailed feedback
            if ("EDIT".equals(feedbackType)) {
                feedbackXP = 10; // Edited predictions provide valuable learning
            }
            
            gamificationService.addXp(userId, feedbackXP);
            
            // Create XP log entry
            XPLogRequest xpRequest = new XPLogRequest();
            xpRequest.setAccountId(userId);
            xpRequest.setXpAmount(feedbackXP);
            xpRequest.setSourceType("AI_FEEDBACK_PROVIDED");
            xpRequest.setDescription(String.format("Provided %s feedback to improve AI predictions", feedbackType.toLowerCase()));
            xpLogService.create(xpRequest);
            
            log.info("Awarded {} XP to user {} for providing %s feedback", feedbackXP, userId, feedbackType);
            
        } catch (Exception e) {
            log.error("Error awarding XP for feedback for userId: {}", userId, e);
        }
    }
}
