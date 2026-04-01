package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.dto.BudgetRequest;
import fingenie.com.fingenie.dto.BudgetResponse;
import fingenie.com.fingenie.dto.BudgetResponse.BudgetAlert;
import fingenie.com.fingenie.dto.BudgetResponse.BudgetSummary;
import fingenie.com.fingenie.entity.Budget.PeriodType;
import fingenie.com.fingenie.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budget", description = "Budget management APIs")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create a new budget")
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(budgetService.createBudget(accountId, request));
    }

    @GetMapping
    @Operation(summary = "Get all active budgets")
    public ResponseEntity<List<BudgetResponse>> getBudgets() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(budgetService.getBudgets(accountId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get budget summary with overall status")
    public ResponseEntity<BudgetSummary> getBudgetSummary() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(budgetService.getBudgetSummary(accountId));
    }

    @GetMapping("/period/{periodType}")
    @Operation(summary = "Get budgets by period type")
    public ResponseEntity<List<BudgetResponse>> getBudgetsByPeriod(
            @PathVariable PeriodType periodType) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(budgetService.getBudgetsByPeriod(accountId, periodType));
    }

    @GetMapping("/{budgetId}")
    @Operation(summary = "Get a specific budget")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable Long budgetId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(budgetService.getBudgetById(accountId, budgetId));
    }

    @PutMapping("/{budgetId}")
    @Operation(summary = "Update a budget")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(budgetService.updateBudget(accountId, budgetId, request));
    }

    @DeleteMapping("/{budgetId}")
    @Operation(summary = "Delete (deactivate) a budget")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long budgetId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        budgetService.deleteBudget(accountId, budgetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get current budget alerts")
    public ResponseEntity<List<BudgetAlert>> getBudgetAlerts() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(budgetService.checkBudgetAlerts(accountId));
    }
}
