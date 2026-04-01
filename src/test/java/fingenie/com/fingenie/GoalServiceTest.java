package fingenie.com.fingenie;

import fingenie.com.fingenie.dto.PiggyGoalRequest;
import fingenie.com.fingenie.dto.PiggyGoalResponse;
import fingenie.com.fingenie.entity.Goal;
import fingenie.com.fingenie.repository.PiggyGoalRepository;
import fingenie.com.fingenie.service.PiggyGoalService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class GoalServiceTest {

    @Test
    void createListAndDeposit() {
        PiggyGoalRepository repo = Mockito.mock(PiggyGoalRepository.class);
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
        PiggyGoalService svc = new PiggyGoalService(repo, publisher);

        PiggyGoalRequest req = PiggyGoalRequest.builder()
                .title("Vacation")
                .targetAmount(new BigDecimal("1000"))
                .deadline(Instant.now())
                .build();

        Goal saved = new Goal();
        saved.setId(1L);
        saved.setAccountId(10L);
        saved.setTitle(req.getTitle());
        saved.setTargetAmount(req.getTargetAmount());
        saved.setCurrentAmount(BigDecimal.ZERO);
        saved.setDeadline(req.getDeadline());

        Mockito.when(repo.save(any(Goal.class))).thenReturn(saved);
        Mockito.when(repo.findByAccountIdOrderByDeadlineAsc(10L)).thenReturn(List.of(saved));
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(saved));

        PiggyGoalResponse created = svc.createGoal(10L, req);
        assertNotNull(created);
        assertEquals(1L, created.getId());

        List<PiggyGoalResponse> list = svc.listGoals(10L);
        assertEquals(1, list.size());

        PiggyGoalResponse.DepositResult afterDeposit = svc.deposit(10L, 1L, new BigDecimal("100"));
        assertNotNull(afterDeposit);
    }
}
