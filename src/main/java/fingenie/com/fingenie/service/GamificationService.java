package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.GamificationProfileDto;
import fingenie.com.fingenie.entity.UserGamification;
import fingenie.com.fingenie.event.PiggyGoalDepositedEvent;
import fingenie.com.fingenie.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserGamificationRepository repository;

    /**
     * Get gamification profile for an account.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public GamificationProfileDto getProfile(Long accountId) {
        UserGamification ug = repository.findByAccountId(accountId).orElse(
                UserGamification.builder().accountId(accountId).xp(0).level(1).build()
        );
        return GamificationProfileDto.builder()
                .accountId(ug.getAccountId())
                .xp(ug.getXp())
                .level(ug.getLevel())
                .build();
    }

    @Transactional
    public GamificationProfileDto addXp(Long accountId, int xpToAdd) {
        UserGamification ug = repository.findByAccountId(accountId).orElse(
                UserGamification.builder().accountId(accountId).xp(0).level(1).build()
        );
        int newXp = ug.getXp() + xpToAdd;
        int newLevel = computeLevelFromXp(newXp);
        ug.setXp(newXp);
        ug.setLevel(newLevel);
        repository.save(ug);
        return GamificationProfileDto.builder()
                .accountId(ug.getAccountId())
                .xp(ug.getXp())
                .level(ug.getLevel())
                .build();
    }

    private int computeLevelFromXp(int xp) {
        // Simple formula: 100 XP per level
        return Math.max(1, xp / 100 + 1);
    }

    @Async
    @EventListener
    public void onPiggyDeposited(PiggyGoalDepositedEvent evt) {
        try {
            Long accountId = evt.getAccountId();
            // award XP proportional to deposit amount: 1 XP per 10 currency units
            int xp = evt.getAmount().divide(BigDecimal.TEN).intValue();
            if (xp <= 0) xp = 1;
            addXp(accountId, xp);
        } catch (Exception ex) {
            // log and ignore
        }
    }
}
