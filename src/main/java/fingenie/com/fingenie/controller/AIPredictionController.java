package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.AIPredictionDto;
import fingenie.com.fingenie.dto.MonthlySavingPlanAdviceResponse;
import fingenie.com.fingenie.dto.MonthlySavingPlanResponse;
import fingenie.com.fingenie.dto.SavingCapacityResponse;
import fingenie.com.fingenie.dto.SavingTarget;
import fingenie.com.fingenie.service.AIPredictionService;
import fingenie.com.fingenie.service.MonthlySavingPlanAdviceService;
import fingenie.com.fingenie.service.MonthlySavingPlanService;
import fingenie.com.fingenie.service.SavingCapacityService;
import fingenie.com.fingenie.service.SavingTargetService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api-prefix}/ai")
@RequiredArgsConstructor
@Tag(name = "AI Predictions", description = "AI prediction and feedback APIs")
public class AIPredictionController {

    private final AIPredictionService predictionService;
    private final SavingCapacityService savingCapacityService;
    private final MonthlySavingPlanAdviceService monthlySavingPlanAdviceService;
    private final MonthlySavingPlanService monthlySavingPlanService;
    private final SavingTargetService savingTargetService;

    @Operation(summary = "Generate monthly prediction")
    @PostMapping("/predict/monthly")
    public ResponseEntity<AIPredictionDto> predictMonthly(@RequestParam("accountId") Long accountId) {
        Long principalAccountId = SecurityUtils.getCurrentAccountId();
        AIPredictionDto dto = predictionService.predictMonthly(principalAccountId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get latest prediction for user")
    @GetMapping("/predictions/latest")
    public ResponseEntity<AIPredictionDto> getLatest(@RequestParam("accountId") Long accountId) {
        Long principalAccountId = SecurityUtils.getCurrentAccountId();
        AIPredictionDto dto = predictionService.getLatest(principalAccountId);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get overspending risk for user")
    @GetMapping("/risk/overspending")
    public ResponseEntity<String> getOverspendingRisk(@RequestParam("accountId") Long accountId) {
        Long principalAccountId = SecurityUtils.getCurrentAccountId();
        AIPredictionDto dto = predictionService.getLatest(principalAccountId);
        String risk = dto == null ? "UNKNOWN" : (dto.getPredictionJson().contains("LOW") ? "LOW" : "MEDIUM/HIGH");
        return ResponseEntity.ok(risk);
    }

    @Operation(summary = "Get deterministic saving capacity estimate")
    @GetMapping("/saving-capacity")
    public ResponseEntity<SavingCapacityResponse> getSavingCapacity() {
        Long principalAccountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(savingCapacityService.calculateSavingCapacity(principalAccountId));
    }

    @Operation(summary = "Get user saving targets across goals and piggies")
    @GetMapping("/saving-targets")
    public ResponseEntity<List<SavingTarget>> getSavingTargets() {
        Long principalAccountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(savingTargetService.getUserSavingTargets(principalAccountId));
    }

    @Operation(summary = "Build monthly saving plan based on capacity and targets")
    @GetMapping("/monthly-saving-plan")
    public ResponseEntity<MonthlySavingPlanResponse> getMonthlySavingPlan(
            @RequestParam(name = "mode", required = false) String mode) {
        Long principalAccountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(monthlySavingPlanService.getMonthlySavingPlan(principalAccountId, mode));
    }

    @Operation(summary = "Get AI advisor explanation for monthly saving plan")
    @PostMapping("/monthly-saving-plan/advice")
    public ResponseEntity<MonthlySavingPlanAdviceResponse> getMonthlySavingPlanAdvice(
            @RequestParam(name = "language", required = false) String language) {
        Long principalAccountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(monthlySavingPlanAdviceService.getMonthlySavingPlanAdvice(principalAccountId, language));
    }
}
