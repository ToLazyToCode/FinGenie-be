package fingenie.com.fingenie.ai.service;

import fingenie.com.fingenie.ai.event.BehaviorTrainingEvent;
import fingenie.com.fingenie.entity.PredictionFeedback;
import fingenie.com.fingenie.entity.UserAISpendingProfile;
import fingenie.com.fingenie.repository.PredictionFeedbackRepository;
import fingenie.com.fingenie.repository.UserAISpendingProfileRepository;
import fingenie.com.fingenie.repository.UserSpendingRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AITrainingService {

    private final UserAISpendingProfileRepository profileRepository;
    private final PredictionFeedbackRepository feedbackRepository;
    private final UserSpendingRoutineRepository routineRepository;

    /**
     * Process behavior training events in real-time
     */
    @EventListener
    @Transactional
    public void processTrainingEvent(BehaviorTrainingEvent event) {
        try {
            log.info("Processing training event for userId: {}, type: {}", 
                event.getAccountId(), event.getFeedbackType());

            // Get or create user profile
            UserAISpendingProfile profile = getOrCreateProfile(event.getAccountId());
            
            // Update profile based on feedback
            updateProfileFromFeedback(profile, event);
            
            // Update confidence score
            updateConfidenceScore(profile);
            
            // Save updated profile
            profileRepository.save(profile);
            
            // Detect or update spending routines
            updateSpendingRoutines(event);
            
        } catch (Exception e) {
            log.error("Error processing training event for userId: {}", event.getAccountId(), e);
        }
    }

    /**
     * Nightly batch training to rebuild full profiles
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    public void performNightlyBatchTraining() {
        log.info("Starting nightly batch training for all user profiles");
        
        List<UserAISpendingProfile> allProfiles = profileRepository.findAll();
        
        for (UserAISpendingProfile profile : allProfiles) {
            try {
                performBatchTrainingForUser(profile);
            } catch (Exception e) {
                log.error("Error in batch training for userId: {}", profile.getUserId(), e);
            }
        }
        
        log.info("Completed nightly batch training for {} users", allProfiles.size());
    }

    private UserAISpendingProfile getOrCreateProfile(Long userId) {
        return profileRepository.findByUserId(userId)
            .orElseGet(() -> {
                UserAISpendingProfile newProfile = new UserAISpendingProfile();
                newProfile.setUserId(userId);
                newProfile.setLastTrainedAt(LocalDateTime.now());
                return newProfile;
            });
    }

    private void updateProfileFromFeedback(UserAISpendingProfile profile, BehaviorTrainingEvent event) {
        String feedbackType = event.getFeedbackType();
        
        // Update prediction counters
        profile.setTotalPredictions(profile.getTotalPredictions() + 1);
        
        switch (feedbackType.toUpperCase()) {
            case "ACCEPT":
                profile.setAcceptedPredictions(profile.getAcceptedPredictions() + 1);
                // Increase confidence for accurate predictions
                profile.setConfidenceScore(
                    profile.getConfidenceScore().add(new BigDecimal("0.01"))
                        .min(new BigDecimal("1.00"))
                );
                break;
                
            case "REJECT":
                profile.setRejectedPredictions(profile.getRejectedPredictions() + 1);
                // Decrease confidence for rejected predictions
                profile.setConfidenceScore(
                    profile.getConfidenceScore().subtract(new BigDecimal("0.02"))
                        .max(new BigDecimal("0.10"))
                );
                break;
                
            case "EDIT":
                profile.setEditedPredictions(profile.getEditedPredictions() + 1);
                // Slightly decrease confidence for edited predictions
                profile.setConfidenceScore(
                    profile.getConfidenceScore().subtract(new BigDecimal("0.005"))
                        .max(new BigDecimal("0.10"))
                );
                break;
        }
        
        // Update accuracy rate
        if (profile.getTotalPredictions() > 0) {
            BigDecimal accuracy = new BigDecimal(profile.getAcceptedPredictions())
                .divide(new BigDecimal(profile.getTotalPredictions()), 2, RoundingMode.HALF_UP);
            profile.setPredictionAccuracyRate(accuracy);
        }
        
        profile.setLastTrainedAt(LocalDateTime.now());
    }

    private void updateConfidenceScore(UserAISpendingProfile profile) {
        // Recalculate confidence based on recent performance
        int total = profile.getTotalPredictions();
        if (total < 5) return; // Not enough data
        
        // Weight recent feedback more heavily
        List<PredictionFeedback> recentFeedback = feedbackRepository
            .findByUserIdOrderByCreatedAtDesc(profile.getUserId())
            .stream()
            .limit(20)
            .toList();
        
        if (recentFeedback.isEmpty()) return;
        
        long accepts = recentFeedback.stream()
            .filter(pf -> "ACCEPT".equals(pf.getFeedbackType()))
            .count();
        
        double recentAccuracy = (double) accepts / recentFeedback.size();
        
        // Blend recent accuracy with existing confidence
        BigDecimal newConfidence = profile.getConfidenceScore()
            .multiply(new BigDecimal("0.7"))
            .add(new BigDecimal(recentAccuracy).multiply(new BigDecimal("0.3")));
        
        profile.setConfidenceScore(newConfidence);
    }

    private void updateSpendingRoutines(BehaviorTrainingEvent event) {
        // This would analyze transaction patterns and update routines
        // For now, just log that it would happen
        log.debug("Would update spending routines for userId: {}", event.getAccountId());
    }

    private void performBatchTrainingForUser(UserAISpendingProfile profile) {
        Long userId = profile.getUserId();
        
        // Get last 30 days of feedback
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<PredictionFeedback> feedback = feedbackRepository
            .findByUserIdAndCreatedAtAfter(userId, Timestamp.valueOf(thirtyDaysAgo));
        
        if (feedback.isEmpty()) return;
        
        // Recalculate all metrics
        Map<String, Long> feedbackCounts = new HashMap<>();
        feedbackCounts.put("ACCEPT", 0L);
        feedbackCounts.put("REJECT", 0L);
        feedbackCounts.put("EDIT", 0L);
        
        for (PredictionFeedback pf : feedback) {
            feedbackCounts.merge(pf.getFeedbackType(), 1L, Long::sum);
        }
        
        profile.setTotalPredictions(feedback.size());
        profile.setAcceptedPredictions(feedbackCounts.get("ACCEPT").intValue());
        profile.setRejectedPredictions(feedbackCounts.get("REJECT").intValue());
        profile.setEditedPredictions(feedbackCounts.get("EDIT").intValue());
        
        // Recalculate accuracy
        if (profile.getTotalPredictions() > 0) {
            BigDecimal accuracy = new BigDecimal(profile.getAcceptedPredictions())
                .divide(new BigDecimal(profile.getTotalPredictions()), 2, RoundingMode.HALF_UP);
            profile.setPredictionAccuracyRate(accuracy);
        }
        
        // Recalculate confidence based on batch performance
        double batchAccuracy = profile.getTotalPredictions() > 0 ? 
            (double) profile.getAcceptedPredictions() / profile.getTotalPredictions() : 0.5;
        
        profile.setConfidenceScore(new BigDecimal(batchAccuracy));
        
        // Detect spending patterns
        detectAndUpdateSpendingPatterns(userId, profile);
        
        profile.setLastTrainedAt(LocalDateTime.now());
        profileRepository.save(profile);
    }

    private void detectAndUpdateSpendingPatterns(Long userId, UserAISpendingProfile profile) {
        // This would analyze transaction history to detect patterns
        // For now, just update category probabilities as a placeholder
        
        Map<String, BigDecimal> categoryProbabilities = new HashMap<>();
        categoryProbabilities.put("FOOD", new BigDecimal("0.30"));
        categoryProbabilities.put("TRANSPORT", new BigDecimal("0.20"));
        categoryProbabilities.put("ENTERTAINMENT", new BigDecimal("0.15"));
        categoryProbabilities.put("SHOPPING", new BigDecimal("0.15"));
        categoryProbabilities.put("OTHER", new BigDecimal("0.20"));
        
        // Convert to JSON string for storage
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, BigDecimal> entry : categoryProbabilities.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":")
                .append(entry.getValue().toString());
            first = false;
        }
        json.append("}");
        
        profile.setCategoryProbabilityMap(json.toString());
    }
}
