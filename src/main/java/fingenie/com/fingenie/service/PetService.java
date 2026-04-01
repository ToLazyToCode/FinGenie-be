package fingenie.com.fingenie.service;

import fingenie.com.fingenie.ai.runtime.AIRuntimeService;
import fingenie.com.fingenie.dto.PetChatRequest;
import fingenie.com.fingenie.dto.PetChatResponse;
import fingenie.com.fingenie.dto.PetProfileResponse;
import fingenie.com.fingenie.dto.PetRequest;
import fingenie.com.fingenie.dto.PetResponse;
import fingenie.com.fingenie.entity.Pet;
import fingenie.com.fingenie.entity.PetProfile;
import fingenie.com.fingenie.event.TransactionCreatedEvent;
import fingenie.com.fingenie.repository.PetProfileRepository;
import fingenie.com.fingenie.repository.PetRepository;
import fingenie.com.fingenie.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing virtual pet state and interactions.
 * OSIV-SAFE: All public methods return DTOs, entity-to-DTO mapping occurs within transaction.
 */
@Service
public class PetService {

    private static final Logger log = LoggerFactory.getLogger(PetService.class);

    private final PetProfileRepository repository;
    private final PetRepository petRepository;
    private final AIRuntimeService aiRuntimeService;

    public PetService(PetProfileRepository repository, PetRepository petRepository, @Lazy AIRuntimeService aiRuntimeService) {
        this.repository = repository;
        this.petRepository = petRepository;
        this.aiRuntimeService = aiRuntimeService;
    }

    /**
     * Get pet profile state as DTO.
     */
    @Transactional(readOnly = true)
    public PetProfileResponse getState(Long accountId) {
        return repository.findByAccountId(accountId)
                .map(PetProfileResponse::fromEntity)
                .orElse(PetProfileResponse.defaultProfile(accountId));
    }

    /**
     * Get pet profile entity for internal service use within transactions.
     * INTERNAL USE ONLY - do not expose outside service layer.
     */
    @Transactional(readOnly = true)
    public PetProfile getStateEntity(Long accountId) {
        return repository.findByAccountId(accountId).orElse(null);
    }

    /**
     * Save pet profile entity. For internal service use.
     */
    @Transactional
    public void savePetProfile(PetProfile petProfile) {
        repository.save(petProfile);
    }

    /**
     * Feed the pet - reduces hunger, increases happiness.
     */
    @Transactional
    public PetProfileResponse feed(Long accountId) {
        PetProfile p = repository.findByAccountId(accountId).orElse(
                PetProfile.builder().accountId(accountId).mood(50).energy(50).hunger(50).happiness(50).build()
        );
        p.setHunger(Math.max(0, p.getHunger() - 20));
        p.setHappiness(Math.min(100, p.getHappiness() + 10));
        PetProfile saved = repository.save(p);
        return PetProfileResponse.fromEntity(saved);
    }

    /**
     * Play with the pet - increases energy and happiness.
     */
    @Transactional
    public PetProfileResponse play(Long accountId) {
        PetProfile p = repository.findByAccountId(accountId).orElse(
                PetProfile.builder().accountId(accountId).mood(50).energy(50).hunger(50).happiness(50).build()
        );
        p.setEnergy(Math.min(100, p.getEnergy() + 10));
        p.setHappiness(Math.min(100, p.getHappiness() + 5));
        PetProfile saved = repository.save(p);
        return PetProfileResponse.fromEntity(saved);
    }

    /**
     * Chat with the virtual pet - AI-powered conversational interaction
     */
    @Transactional
    public PetChatResponse chat(PetChatRequest request) {
        Long accountId = SecurityUtils.getCurrentAccount().getId();
        
        // Get pet profile entity within transaction (not using getState() which returns DTO)
        PetProfile petProfile = repository.findByAccountId(accountId).orElse(
                PetProfile.builder().accountId(accountId).mood(50).energy(50).hunger(50).happiness(50).build()
        );
        Pet pet = petRepository.findByAccountId(accountId).orElse(null);
        
        String personality = pet != null ? pet.getPersonality() : "friendly";
        String mood = deriveMoodFromProfile(petProfile);
        
        // Build pet personality context
        List<String> extras = List.of(
            "pet_personality=" + personality,
            "pet_mood=" + mood,
            "pet_happiness=" + petProfile.getHappiness(),
            "pet_energy=" + petProfile.getEnergy()
        );
        
        // Generate AI response with pet personality
        var aiResponse = aiRuntimeService.generateWithMemory(
            accountId, 
            "PET_CHAT", 
            request.getMessage(),
            extras
        );
        
        // Chatting increases happiness slightly
        petProfile.setHappiness(Math.min(100, petProfile.getHappiness() + 2));
        repository.save(petProfile);
        
        return PetChatResponse.builder()
            .petMessage(aiResponse.getText())
            .mood(mood)
            .happiness(petProfile.getHappiness())
            .confidence(aiResponse.getConfidence())
            .personality(personality)
            .build();
    }

    private String deriveMoodFromProfile(PetProfile profile) {
        int avgScore = (profile.getMood() + profile.getHappiness() + profile.getEnergy()) / 3;
        if (avgScore >= 70) return "happy";
        if (avgScore >= 50) return "content";
        if (avgScore >= 30) return "tired";
        return "sad";
    }

    /**
     * Async event handler for transaction creation.
     * OSIV-SAFE: Uses primitive data from event, no entity access.
     */
    @Async
    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent evt) {
        // Simplified asynchronous reaction: if transaction amount > 1000 -> reduce mood
        try {
            Long accountId = evt.getAccountId();
            PetProfile p = repository.findByAccountId(accountId).orElse(
                    PetProfile.builder().accountId(accountId).mood(50).energy(50).hunger(50).happiness(50).build()
            );
            double amount = evt.getAmount() != null ? evt.getAmount().doubleValue() : 0;
            if (amount > 1000) {
                p.setMood(Math.max(0, p.getMood() - 10));
                p.setHappiness(Math.max(0, p.getHappiness() - 5));
            } else {
                p.setHappiness(Math.min(100, p.getHappiness() + 2));
            }
            repository.save(p);
        } catch (Exception ex) {
            log.error("Error processing TransactionCreatedEvent for account {}", evt.getAccountId(), ex);
        }
    }

    // ========== Pet CRUD Operations ==========

    @Transactional
    public PetResponse create(PetRequest request) {
        Pet pet = new Pet();
        pet.setAccountId(request.getAccountId());
        pet.setPetName(request.getPetName());
        pet.setPetType(request.getPetType());
        pet.setPetAvatarUrl(request.getPetAvatarUrl());
        pet.setPersonality(request.getPersonality());
        pet.setLevel(1);
        pet.setExperiencePoints(0);
        pet.setMood("happy");
        return toResponse(petRepository.save(pet));
    }

    @Transactional(readOnly = true)
    public List<PetResponse> getAll() {
        return petRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PetResponse getById(Long id) {
        return petRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Pet not found"));
    }

    @Transactional
    public void delete(Long id) {
        petRepository.deleteById(id);
    }

    private PetResponse toResponse(Pet pet) {
        PetResponse res = new PetResponse();
        res.setPetId(pet.getId());
        res.setAccountId(pet.getAccountId());
        res.setPetName(pet.getPetName());
        res.setPetType(pet.getPetType());
        res.setPetAvatarUrl(pet.getPetAvatarUrl());
        res.setLevel(pet.getLevel());
        res.setExperiencePoints(pet.getExperiencePoints());
        res.setMood(pet.getMood());
        res.setPersonality(pet.getPersonality());
        return res;
    }
}
