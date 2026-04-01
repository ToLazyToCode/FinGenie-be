package fingenie.com.fingenie;

import fingenie.com.fingenie.dto.AIPredictionDto;
import fingenie.com.fingenie.entity.AIPrediction;
import fingenie.com.fingenie.repository.AIPredictionRepository;
import fingenie.com.fingenie.service.AIPredictionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

public class AIPredictionServiceTest {

    @Test
    void predictAndRetrieve() {
        AIPredictionRepository repo = Mockito.mock(AIPredictionRepository.class);
        Mockito.when(repo.save(any(AIPrediction.class))).thenAnswer(i -> {
            AIPrediction in = i.getArgument(0);
            in.setId(1L);
            return in;
        });
        
        AIPrediction existingPrediction = new AIPrediction();
        existingPrediction.setId(1L);
        existingPrediction.setAccountId(1L);
        existingPrediction.setPredictionJson("{}");
        
        Mockito.when(repo.findTopByAccountIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(existingPrediction));

        AIPredictionService svc = new AIPredictionService(repo);
        AIPredictionDto dto = svc.predictMonthly(1L);
        assertNotNull(dto);
        AIPredictionDto latest = svc.getLatest(1L);
        assertNotNull(latest);
    }
}
