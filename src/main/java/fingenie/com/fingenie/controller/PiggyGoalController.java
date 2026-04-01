package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.dto.PiggyGoalRequest;
import fingenie.com.fingenie.dto.PiggyGoalResponse;
import fingenie.com.fingenie.dto.PiggyGoalResponse.DepositResult;
import fingenie.com.fingenie.dto.PiggyGoalResponse.PiggyGoalSummary;
import fingenie.com.fingenie.service.PiggyGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("${api-prefix}/piggy-goals")
@RequiredArgsConstructor
@Tag(name = "Piggy Goals", description = "Savings goals management APIs")
public class PiggyGoalController {

    private final PiggyGoalService service;

    @PostMapping
    @Operation(summary = "Create a new savings goal")
    public ResponseEntity<PiggyGoalResponse> createGoal(@RequestBody PiggyGoalRequest req) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        PiggyGoalResponse created = service.createGoal(accountId, req);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(summary = "List all savings goals")
    public ResponseEntity<List<PiggyGoalResponse>> listGoals() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        List<PiggyGoalResponse> goals = service.listGoals(accountId);
        return ResponseEntity.ok(goals);
    }
    
    @GetMapping("/summary")
    @Operation(summary = "Get savings goals summary with overall progress")
    public ResponseEntity<PiggyGoalSummary> getGoalSummary() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(service.getGoalSummary(accountId));
    }
    
    @GetMapping("/{goalId}")
    @Operation(summary = "Get a specific savings goal")
    public ResponseEntity<PiggyGoalResponse> getGoal(@PathVariable Long goalId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(service.getGoalById(accountId, goalId));
    }

    @PostMapping("/{goalId}/deposit")
    @Operation(summary = "Deposit money into a savings goal")
    public ResponseEntity<DepositResult> deposit(
            @PathVariable Long goalId,
            @RequestParam BigDecimal amount) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        DepositResult result = service.deposit(accountId, goalId, amount);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{goalId}/withdraw")
    @Operation(summary = "Withdraw money from a savings goal")
    public ResponseEntity<DepositResult> withdraw(
            @PathVariable Long goalId,
            @RequestParam BigDecimal amount) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        DepositResult result = service.withdraw(accountId, goalId, amount);
        return ResponseEntity.ok(result);
    }
    
    @PutMapping("/{goalId}")
    @Operation(summary = "Update a savings goal")
    public ResponseEntity<PiggyGoalResponse> updateGoal(
            @PathVariable Long goalId,
            @RequestBody PiggyGoalRequest req) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(service.updateGoal(accountId, goalId, req));
    }
    
    @DeleteMapping("/{goalId}")
    @Operation(summary = "Delete a savings goal")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long goalId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        service.deleteGoal(accountId, goalId);
        return ResponseEntity.noContent().build();
    }
}
