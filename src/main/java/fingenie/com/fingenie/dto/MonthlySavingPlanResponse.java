package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySavingPlanResponse {

    private BigDecimal savingCapacity;
    private BigDecimal totalRequired;
    private BigDecimal overallFeasibilityScore;

    @Builder.Default
    private List<Allocation> allocations = Collections.emptyList();

    @Builder.Default
    private List<Recommendation> recommendations = Collections.emptyList();

    @Builder.Default
    private List<WhatIfScenario> whatIfScenarios = Collections.emptyList();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Allocation {
        private SavingTarget.TargetType type;
        private Long id;
        private String title;
        private BigDecimal requiredMonthly;
        private BigDecimal allocatedMonthly;
        private BigDecimal feasibilityScore;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String type;
        private String message;
        private BigDecimal impactMonthly;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatIfScenario {
        private String assumption;
        private BigDecimal newSavingCapacity;
        private BigDecimal newFeasibilityScore;
    }
}
