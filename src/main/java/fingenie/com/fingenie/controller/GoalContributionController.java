package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.entity.SavingContribution;
import fingenie.com.fingenie.service.SavingContributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/goals")
@RequiredArgsConstructor
public class GoalContributionController {

    private final SavingContributionService savingContributionService;

    @PostMapping("/{goalId}/contribute")
    public ResponseEntity<Map<String, Object>> contribute(
            @PathVariable Long goalId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "MANUAL") SavingContribution.Source source
    ) {
        SavingContribution contribution = savingContributionService.addGoalContribution(goalId, amount, source);
        return ResponseEntity.ok(Map.of(
                "contributionId", contribution.getId(),
                "goalId", goalId,
                "amount", contribution.getAmount(),
                "source", contribution.getSource(),
                "createdAt", contribution.getCreatedAt()
        ));
    }
}
