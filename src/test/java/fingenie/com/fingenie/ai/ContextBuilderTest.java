package fingenie.com.fingenie.ai;

import fingenie.com.fingenie.ai.builder.ContextBuilder;
import fingenie.com.fingenie.dto.AIPredictionDto;
import fingenie.com.fingenie.dto.GamificationProfileDto;
import fingenie.com.fingenie.dto.PetProfileResponse;
import fingenie.com.fingenie.entity.Goal;
import fingenie.com.fingenie.service.AIPredictionService;
import fingenie.com.fingenie.service.GamificationService;
import fingenie.com.fingenie.service.PetService;
import fingenie.com.fingenie.service.PiggyGoalService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class ContextBuilderTest {

    @Test
    void buildSummaryIncludesAllSections() {
        AIPredictionService pred = Mockito.mock(AIPredictionService.class);
        GamificationService gam = Mockito.mock(GamificationService.class);
        PetService pet = Mockito.mock(PetService.class);
        PiggyGoalService piggy = Mockito.mock(PiggyGoalService.class);

        when(pred.getLatest(anyLong())).thenReturn(AIPredictionDto.builder().id(1L).accountId(1L).predictionJson("{monthly:100}").build());
        when(gam.getProfile(anyLong())).thenReturn(GamificationProfileDto.builder().accountId(1L).xp(10).level(2).build());
        PetProfileResponse petProfile = new PetProfileResponse();
        petProfile.setId(1L);
        when(pet.getState(anyLong())).thenReturn(petProfile);
        
        // Use listGoalEntities() since ContextBuilder now uses that method
        Goal goal = new Goal();
        goal.setId(1L);
        goal.setAccountId(1L);
        goal.setTitle("Vacation");
        goal.setTargetAmount(new BigDecimal("1000"));
        goal.setCurrentAmount(new BigDecimal("100"));
        goal.setDeadline(Instant.now());
        when(piggy.listGoalEntities(anyLong())).thenReturn(List.of(goal));

        ContextBuilder cb = new ContextBuilder(pred, gam, pet, piggy);
        String summary = cb.buildFinancialSummary(1L);
        assertTrue(summary.contains("LatestPrediction"));
        assertTrue(summary.contains("Gamification"));
        assertTrue(summary.contains("Pet:"));
        assertTrue(summary.contains("PiggyGoals"));
    }
}
