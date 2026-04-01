package fingenie.com.fingenie.ai.builder;

import fingenie.com.fingenie.dto.AIPredictionDto;
import fingenie.com.fingenie.dto.GamificationProfileDto;
import fingenie.com.fingenie.entity.PetProfile;
import fingenie.com.fingenie.entity.Goal;
import fingenie.com.fingenie.service.AIPredictionService;
import fingenie.com.fingenie.service.GamificationService;
import fingenie.com.fingenie.service.PetService;
import fingenie.com.fingenie.service.PiggyGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContextBuilder {

    private final AIPredictionService predictionService;
    private final GamificationService gamificationService;
    private final PetService petService;
    private final PiggyGoalService piggyGoalService;

    /**
     * Build a sanitized financial summary for AI prompts. Important: Do not include raw transaction lists or sensitive data.
     */
    public String buildFinancialSummary(Long accountId) {
        StringBuilder sb = new StringBuilder();
        sb.append("FinancialSummary for accountId=").append(accountId).append("\n");

        // Prediction summary
        try {
            AIPredictionDto pred = predictionService.getLatest(accountId);
            if (pred != null && pred.getPredictionJson() != null) {
                sb.append("- LatestPrediction: ").append(sanitize(pred.getPredictionJson())).append("\n");
            } else {
                sb.append("- LatestPrediction: none\n");
            }
        } catch (Exception ex) {
            sb.append("- LatestPrediction: error\n");
        }

        // Gamification summary
        try {
            GamificationProfileDto g = gamificationService.getProfile(accountId);
            if (g != null) {
                sb.append("- Gamification: level=").append(g.getLevel()).append(" xp=").append(g.getXp()).append("\n");
            } else {
                sb.append("- Gamification: none\n");
            }
        } catch (Exception ex) {
            sb.append("- Gamification: error\n");
        }

        // Pet state
        try {
            PetProfile pet = petService.getStateEntity(accountId);
            if (pet != null) {
                sb.append("- Pet: mood=").append(pet.getMood()).append(" happiness=").append(pet.getHappiness()).append("\n");
            } else {
                sb.append("- Pet: none\n");
            }
        } catch (Exception ex) {
            sb.append("- Pet: error\n");
        }

        // Piggy goals summary
        try {
            List<Goal> goals = piggyGoalService.listGoalEntities(accountId);
            if (goals != null && !goals.isEmpty()) {
                sb.append("- PiggyGoals: count=").append(goals.size()).append("\n");
                String brief = goals.stream().map(g -> {
                    BigDecimal target = g.getTargetAmount() == null ? BigDecimal.ZERO : g.getTargetAmount();
                    BigDecimal current = g.getCurrentAmount() == null ? BigDecimal.ZERO : g.getCurrentAmount();
                    return String.format("%s: %s/%s", safe(g.getTitle()), current.toPlainString(), target.toPlainString());
                }).collect(Collectors.joining("; "));
                sb.append("  ").append(safe(brief)).append("\n");
            } else {
                sb.append("- PiggyGoals: none\n");
            }
        } catch (Exception ex) {
            sb.append("- PiggyGoals: error\n");
        }

        return sb.toString();
    }

    private String safe(String in) {
        if (in == null) return "";
        // simple sanitization: remove newlines and limit length
        String cleaned = in.replaceAll("[\n\r]", " ").trim();
        return cleaned.substring(0, Math.min(200, cleaned.length()));
    }

    private String sanitize(String in) {
        if (in == null) return "";
        return in.replaceAll("\\s+", " ").trim();
    }
}
