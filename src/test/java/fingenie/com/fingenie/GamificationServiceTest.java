package fingenie.com.fingenie;

import fingenie.com.fingenie.entity.UserGamification;
import fingenie.com.fingenie.repository.UserGamificationRepository;
import fingenie.com.fingenie.service.GamificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;

public class GamificationServiceTest {

    @Test
    void addXpIncrementsLevel() {
        UserGamificationRepository repo = Mockito.mock(UserGamificationRepository.class);
        Mockito.when(repo.findById(anyLong())).thenReturn(Optional.empty());
        Mockito.when(repo.save(Mockito.any(UserGamification.class))).thenAnswer(i -> i.getArgument(0));

        GamificationService svc = new GamificationService(repo);
        var profile = svc.addXp(1L, 250);
        assertEquals(3, profile.getLevel()); // 250 xp -> level 3 (100 per level)
    }
}
