package fingenie.com.fingenie.ai.event;

import fingenie.com.fingenie.ai.service.AITrainingService;
import fingenie.com.fingenie.ai.service.FeatureStoreService;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.PetProfile;
import fingenie.com.fingenie.event.TransactionCreatedEvent;
import fingenie.com.fingenie.event.TransactionDeletedEvent;
import fingenie.com.fingenie.event.TransactionUpdatedEvent;
import fingenie.com.fingenie.repository.PetProfileRepository;
import fingenie.com.fingenie.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to AI-related events and coordinates responses across services.
 * Handles:
 * - AI profile updates
 * - Gamification triggers
 * - Pet mood updates
 * - Transaction-based learning
 * 
 * OSIV-SAFE: All event handlers use primitive data from events,
 * no lazy loading occurs outside of explicit @Transactional boundaries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIEventListener {

    private final AITrainingService trainingService;
    private final GamificationService gamificationService;
    private final PetProfileRepository petProfileRepository;
    private final FeatureStoreService featureStoreService;

    private static final int XP_FOR_ACCEPT = 5;
    private static final int XP_FOR_EDIT = 3;
    private static final int XP_FOR_SAVING = 10;
    private static final int XP_FOR_TRANSACTION = 2;

    /**
     * Handles transaction creation events.
     * Uses primitive data from event - no entity access required.
     */
    @Async
    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        Long accountId = event.getAccountId();
        
        log.info("Processing TransactionCreatedEvent for user {} - category: {}, amount: {}", 
                accountId, event.getCategoryName(), event.getAmount());
        
        try {
            // 1. Train AI with actual transaction data (all from event primitives)
            String comment = String.format("category=%s,type=%s,amount=%s,description=%s",
                    event.getCategoryName(),
                    event.getCategoryType(),
                    event.getAmount(),
                    event.getDescription() != null ? event.getDescription() : "");
            
            trainingService.processTrainingEvent(
                new BehaviorTrainingEvent(
                    this,
                    accountId,
                    event.getTransactionId(),
                    "TRANSACTION",
                    comment
                )
            );

            // 2. Award XP based on transaction type (using event's categoryType)
            Category.CategoryType type = event.getCategoryType();
            if (type == Category.CategoryType.SAVING) {
                gamificationService.addXp(accountId, XP_FOR_SAVING);
            } else {
                gamificationService.addXp(accountId, XP_FOR_TRANSACTION);
            }

            // 3. Keep online feature store fresh for AI inference.
            featureStoreService.updateFeatures(accountId);
            
        } catch (Exception e) {
            log.error("Error processing TransactionCreatedEvent for user {}", accountId, e);
        }
    }

    @Async
    @EventListener
    public void onTransactionUpdated(TransactionUpdatedEvent event) {
        Long accountId = event.getAccountId();
        try {
            featureStoreService.updateFeatures(accountId);
            log.info("Updated feature store after transaction update for user {}", accountId);
        } catch (Exception e) {
            log.error("Error processing TransactionUpdatedEvent for user {}", accountId, e);
        }
    }

    @Async
    @EventListener
    public void onTransactionDeleted(TransactionDeletedEvent event) {
        Long accountId = event.getAccountId();
        try {
            featureStoreService.updateFeatures(accountId);
            log.info("Updated feature store after transaction deletion for user {}", accountId);
        } catch (Exception e) {
            log.error("Error processing TransactionDeletedEvent for user {}", accountId, e);
        }
    }

    @Async
    @EventListener
    public void onGuessAccepted(GuessAcceptedEvent event) {
        log.info("Processing GuessAcceptedEvent for user {}", event.getAccountId());
        
        try {
            // 1. Train AI with positive feedback
            trainingService.processTrainingEvent(
                new BehaviorTrainingEvent(
                    this,
                    event.getAccountId(),
                    event.getGuess().getId(),
                    "ACCEPT",
                    "Guess accepted by user"
                )
            );

            // 2. Award XP for accepting AI guess
            gamificationService.addXp(event.getAccountId(), XP_FOR_ACCEPT);

            // 3. Make pet happy
            updatePetMood(event.getAccountId(), 5, 3);
            
        } catch (Exception e) {
            log.error("Error processing GuessAcceptedEvent for user {}", event.getAccountId(), e);
        }
    }

    @Async
    @EventListener
    public void onGuessEdited(GuessEditedEvent event) {
        log.info("Processing GuessEditedEvent for user {} (delta: {})", 
                event.getAccountId(), event.getAmountDelta());
        
        try {
            // 1. Train AI with partial positive feedback
            String comment = String.format("delta=%s,category_changed=%s", 
                    event.getAmountDelta(), event.isCategoryChanged());
            trainingService.processTrainingEvent(
                new BehaviorTrainingEvent(
                    this,
                    event.getAccountId(),
                    event.getGuess().getId(),
                    "EDIT",
                    comment
                )
            );

            // 2. Award smaller XP for editing
            gamificationService.addXp(event.getAccountId(), XP_FOR_EDIT);

            // 3. Pet slightly happy
            updatePetMood(event.getAccountId(), 2, 1);
            
        } catch (Exception e) {
            log.error("Error processing GuessEditedEvent for user {}", event.getAccountId(), e);
        }
    }

    @Async
    @EventListener
    public void onGuessRejected(GuessRejectedEvent event) {
        log.info("Processing GuessRejectedEvent for user {} (reason: {})", 
                event.getAccountId(), event.getReason());
        
        try {
            // 1. Train AI with negative feedback
            trainingService.processTrainingEvent(
                new BehaviorTrainingEvent(
                    this,
                    event.getAccountId(),
                    event.getGuess().getId(),
                    "REJECT",
                    event.getReason()
                )
            );

            // No XP for rejection
            // Pet mood unchanged (rejection is neutral, not negative for pet)
            
        } catch (Exception e) {
            log.error("Error processing GuessRejectedEvent for user {}", event.getAccountId(), e);
        }
    }

    private void updatePetMood(Long accountId, int moodBoost, int happinessBoost) {
        try {
            PetProfile pet = petProfileRepository.findByAccountId(accountId)
                    .orElse(PetProfile.builder()
                            .accountId(accountId)
                            .mood(50)
                            .happiness(50)
                            .energy(50)
                            .hunger(50)
                            .build());

            pet.setMood(Math.min(100, pet.getMood() + moodBoost));
            pet.setHappiness(Math.min(100, pet.getHappiness() + happinessBoost));
            petProfileRepository.save(pet);
        } catch (Exception e) {
            log.error("Error updating pet mood for user {}", accountId, e);
        }
    }
}
