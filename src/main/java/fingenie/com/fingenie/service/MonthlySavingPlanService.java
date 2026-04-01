package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.MonthlySavingPlanResponse;
import fingenie.com.fingenie.dto.SavingCapacityResponse;
import fingenie.com.fingenie.dto.SavingTarget;
import fingenie.com.fingenie.entity.Goal;
import fingenie.com.fingenie.repository.PiggyGoalRepository;
import fingenie.com.fingenie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlySavingPlanService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final int HIGH_PRIORITY_WEIGHT = 3;
    private static final int MEDIUM_PRIORITY_WEIGHT = 2;
    private static final int LOW_PRIORITY_WEIGHT = 1;
    private static final String OPTIMIZED_MODE = "optimized";
    private static final int LOOKBACK_DAYS = 30;
    private static final int TOP_EXPENSE_CATEGORY_LIMIT = 3;
    private static final int[] EXPENSE_REDUCTION_HINTS = {15, 10, 5};
    private static final BigDecimal WHAT_IF_REDUCTION_PERCENT = new BigDecimal("0.10");

    private final SavingCapacityService savingCapacityService;
    private final SavingTargetService savingTargetService;
    private final PiggyGoalRepository piggyGoalRepository;
    private final TransactionRepository transactionRepository;

    public MonthlySavingPlanResponse getMonthlySavingPlan(Long accountId) {
        return getMonthlySavingPlan(accountId, null);
    }

    public MonthlySavingPlanResponse getMonthlySavingPlan(Long accountId, String mode) {
        SavingCapacityResponse capacityResponse = savingCapacityService.calculateSavingCapacity(accountId);
        BigDecimal savingCapacity = normalizeNonNegative(capacityResponse.getSavingCapacity());

        List<SavingTarget> targets = savingTargetService.getUserSavingTargets(accountId);
        Map<Long, Goal.Priority> goalPriorityById = loadGoalPriorityMap(accountId);

        List<AllocationState> states = targets.stream()
                .map(target -> toState(target, goalPriorityById))
                .collect(Collectors.toCollection(ArrayList::new));

        long savingCapacityCents = toCents(savingCapacity);
        long totalRequiredCents = states.stream().mapToLong(AllocationState::getRequiredCents).sum();

        if (totalRequiredCents <= savingCapacityCents) {
            states.forEach(state -> state.setAllocatedCents(state.getRequiredCents()));
        } else {
            allocateByWeight(states, savingCapacityCents);
        }

        BigDecimal totalRequired = fromCents(totalRequiredCents);
        BigDecimal overallFeasibilityScore = computeFeasibilityScore(savingCapacity, totalRequired);

        List<MonthlySavingPlanResponse.Allocation> allocations = states.stream()
                .map(this::toAllocationDto)
                .toList();

        List<MonthlySavingPlanResponse.Recommendation> recommendations = List.of();
        List<MonthlySavingPlanResponse.WhatIfScenario> whatIfScenarios = List.of();

        boolean isShortage = totalRequiredCents > savingCapacityCents;
        if (isShortage && isOptimizedMode(mode)) {
            List<CategorySpend> topExpenseCategories = loadTopExpenseCategoriesLast30Days(accountId);
            recommendations = buildRecommendations(states, topExpenseCategories);
            whatIfScenarios = buildWhatIfScenarios(
                    savingCapacity,
                    totalRequired,
                    topExpenseCategories
            );
        }

        return MonthlySavingPlanResponse.builder()
                .savingCapacity(savingCapacity)
                .totalRequired(totalRequired)
                .overallFeasibilityScore(overallFeasibilityScore)
                .allocations(allocations)
                .recommendations(recommendations)
                .whatIfScenarios(whatIfScenarios)
                .build();
    }

    private Map<Long, Goal.Priority> loadGoalPriorityMap(Long accountId) {
        return piggyGoalRepository.findByAccountId(accountId).stream()
                .collect(Collectors.toMap(
                        Goal::getId,
                        goal -> goal.getPriority() == null ? Goal.Priority.MEDIUM : goal.getPriority(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    private AllocationState toState(SavingTarget target, Map<Long, Goal.Priority> goalPriorityById) {
        Goal.Priority priority = resolvePriority(target, goalPriorityById);
        int priorityWeight = switch (priority) {
            case HIGH -> HIGH_PRIORITY_WEIGHT;
            case MEDIUM -> MEDIUM_PRIORITY_WEIGHT;
            case LOW -> LOW_PRIORITY_WEIGHT;
        };

        int shareWeight = resolvePiggyShareWeight(target);
        int finalWeight = target.getType() == SavingTarget.TargetType.PIGGY
                ? Math.max(1, priorityWeight * shareWeight)
                : priorityWeight;

        long requiredCents = toCents(normalizeNonNegative(target.getRequiredMonthly()));
        String notes = buildNotes(target, priority, shareWeight, finalWeight);

        return new AllocationState(target, requiredCents, finalWeight, notes, priority);
    }

    private Goal.Priority resolvePriority(SavingTarget target, Map<Long, Goal.Priority> goalPriorityById) {
        if (target.getType() != SavingTarget.TargetType.GOAL) {
            return Goal.Priority.MEDIUM;
        }
        if (target.getId() == null) {
            return Goal.Priority.MEDIUM;
        }
        return goalPriorityById.getOrDefault(target.getId(), Goal.Priority.MEDIUM);
    }

    private int resolvePiggyShareWeight(SavingTarget target) {
        if (target.getType() != SavingTarget.TargetType.PIGGY || target.getMetadata() == null) {
            return 1;
        }
        Object raw = target.getMetadata().get("shareWeight");
        if (raw instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (raw instanceof String stringValue) {
            try {
                return Math.max(1, Integer.parseInt(stringValue));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private String buildNotes(SavingTarget target, Goal.Priority priority, int shareWeight, int finalWeight) {
        String base = target.getNotes() == null ? "" : target.getNotes().trim();
        StringBuilder sb = new StringBuilder();
        if (!base.isEmpty()) {
            sb.append(base).append(" | ");
        }
        sb.append("priority=").append(priority.name());
        if (target.getType() == SavingTarget.TargetType.PIGGY) {
            sb.append(", shareWeight=").append(shareWeight);
        }
        sb.append(", allocationWeight=").append(finalWeight);
        return sb.toString();
    }

    private void allocateByWeight(List<AllocationState> states, long savingCapacityCents) {
        if (savingCapacityCents <= 0) {
            return;
        }

        List<AllocationState> positiveRequired = states.stream()
                .filter(state -> state.getRequiredCents() > 0)
                .toList();

        if (positiveRequired.isEmpty()) {
            return;
        }

        long totalWeight = positiveRequired.stream()
                .mapToLong(state -> Math.max(1, state.getWeight()))
                .sum();

        if (totalWeight <= 0) {
            return;
        }

        long allocatedTotal = 0L;
        for (AllocationState state : positiveRequired) {
            double exactShare = (double) savingCapacityCents * (double) state.getWeight() / (double) totalWeight;
            long baseCents = (long) Math.floor(exactShare);
            long cappedBase = Math.min(baseCents, state.getRequiredCents());
            state.setAllocatedCents(cappedBase);
            state.setFractionalRemainder(exactShare - baseCents);
            allocatedTotal += cappedBase;
        }

        long remainingCents = savingCapacityCents - allocatedTotal;
        if (remainingCents <= 0) {
            return;
        }

        List<AllocationState> byRemainder = positiveRequired.stream()
                .sorted(Comparator
                        .comparingDouble(AllocationState::getFractionalRemainder).reversed()
                        .thenComparing(AllocationState::getWeight, Comparator.reverseOrder()))
                .toList();

        remainingCents = distributeOneCentAtATime(byRemainder, remainingCents);
        if (remainingCents <= 0) {
            return;
        }

        List<AllocationState> byGap = positiveRequired.stream()
                .sorted(Comparator
                        .comparingLong((AllocationState state) -> state.getRequiredCents() - state.getAllocatedCents()).reversed()
                        .thenComparing(AllocationState::getWeight, Comparator.reverseOrder()))
                .toList();
        distributeOneCentAtATime(byGap, remainingCents);
    }

    private long distributeOneCentAtATime(List<AllocationState> orderedStates, long remainingCents) {
        while (remainingCents > 0) {
            boolean progressed = false;
            for (AllocationState state : orderedStates) {
                if (remainingCents <= 0) {
                    break;
                }
                if (state.getAllocatedCents() >= state.getRequiredCents()) {
                    continue;
                }
                state.setAllocatedCents(state.getAllocatedCents() + 1);
                remainingCents--;
                progressed = true;
            }
            if (!progressed) {
                break;
            }
        }
        return remainingCents;
    }

    private MonthlySavingPlanResponse.Allocation toAllocationDto(AllocationState state) {
        BigDecimal requiredMonthly = fromCents(state.getRequiredCents());
        BigDecimal allocatedMonthly = fromCents(state.getAllocatedCents());
        BigDecimal feasibilityScore = computeFeasibilityScore(allocatedMonthly, requiredMonthly);

        return MonthlySavingPlanResponse.Allocation.builder()
                .type(state.getTarget().getType())
                .id(state.getTarget().getId())
                .title(state.getTarget().getTitle())
                .requiredMonthly(requiredMonthly)
                .allocatedMonthly(allocatedMonthly)
                .feasibilityScore(feasibilityScore)
                .notes(state.getNotes())
                .build();
    }

    private boolean isOptimizedMode(String mode) {
        return mode != null && OPTIMIZED_MODE.equalsIgnoreCase(mode.trim());
    }

    private List<MonthlySavingPlanResponse.Recommendation> buildRecommendations(
            List<AllocationState> states,
            List<CategorySpend> topExpenseCategories
    ) {
        List<MonthlySavingPlanResponse.Recommendation> recommendations = new ArrayList<>();
        recommendations.addAll(buildStretchDeadlineRecommendations(states));
        recommendations.addAll(buildExpenseReductionRecommendations(topExpenseCategories));
        return recommendations;
    }

    private List<MonthlySavingPlanResponse.Recommendation> buildStretchDeadlineRecommendations(List<AllocationState> states) {
        List<MonthlySavingPlanResponse.Recommendation> recommendations = new ArrayList<>();
        for (AllocationState state : states) {
            if (state.getTarget().getType() != SavingTarget.TargetType.GOAL || state.getPriority() != Goal.Priority.LOW) {
                continue;
            }

            BigDecimal remainingAmount = normalizeNonNegative(state.getTarget().getRemainingAmount());
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocatedMonthly = fromCents(state.getAllocatedCents());
            BigDecimal impactMonthly = fromCents(Math.max(0L, state.getRequiredCents() - state.getAllocatedCents()));
            String title = state.getTarget().getTitle() == null ? "Goal" : state.getTarget().getTitle();

            String message;
            if (allocatedMonthly.compareTo(BigDecimal.ZERO) <= 0) {
                message = "Stretch deadline for LOW priority goal '" + title +
                        "': no monthly allocation is available now; defer this goal until capacity improves.";
            } else {
                long monthsToFinish = remainingAmount
                        .divide(allocatedMonthly, 0, RoundingMode.CEILING)
                        .longValue();
                monthsToFinish = Math.max(1L, monthsToFinish);
                LocalDate projectedDate = LocalDate.now().plusMonths(monthsToFinish);
                message = "Stretch deadline for LOW priority goal '" + title + "' to around " + projectedDate +
                        " with current allocation " + formatAmount(allocatedMonthly) + "/month.";
            }

            recommendations.add(MonthlySavingPlanResponse.Recommendation.builder()
                    .type("STRETCH_DEADLINE")
                    .message(message)
                    .impactMonthly(impactMonthly)
                    .build());
        }
        return recommendations;
    }

    private List<MonthlySavingPlanResponse.Recommendation> buildExpenseReductionRecommendations(List<CategorySpend> topExpenseCategories) {
        List<MonthlySavingPlanResponse.Recommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < topExpenseCategories.size(); i++) {
            CategorySpend categorySpend = topExpenseCategories.get(i);
            int hintPercent = EXPENSE_REDUCTION_HINTS[Math.min(i, EXPENSE_REDUCTION_HINTS.length - 1)];
            BigDecimal impact = calculatePercentageAmount(categorySpend.monthlyExpense(), hintPercent);
            String message = "Reduce '" + categorySpend.categoryName() + "' spending by " + hintPercent +
                    "% to free about " + formatAmount(impact) + " per month.";
            recommendations.add(MonthlySavingPlanResponse.Recommendation.builder()
                    .type("REDUCE_EXPENSE")
                    .message(message)
                    .impactMonthly(impact)
                    .build());
        }
        return recommendations;
    }

    private List<MonthlySavingPlanResponse.WhatIfScenario> buildWhatIfScenarios(
            BigDecimal savingCapacity,
            BigDecimal totalRequired,
            List<CategorySpend> topExpenseCategories
    ) {
        List<MonthlySavingPlanResponse.WhatIfScenario> scenarios = new ArrayList<>();
        for (CategorySpend categorySpend : topExpenseCategories) {
            BigDecimal additionalCapacity = categorySpend.monthlyExpense()
                    .multiply(WHAT_IF_REDUCTION_PERCENT)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal newSavingCapacity = normalizeNonNegative(savingCapacity.add(additionalCapacity));
            BigDecimal newFeasibilityScore = computeFeasibilityScore(newSavingCapacity, totalRequired);
            scenarios.add(MonthlySavingPlanResponse.WhatIfScenario.builder()
                    .assumption("If '" + categorySpend.categoryName() + "' spending decreases by 10%")
                    .newSavingCapacity(newSavingCapacity)
                    .newFeasibilityScore(newFeasibilityScore)
                    .build());
        }
        return scenarios;
    }

    private List<CategorySpend> loadTopExpenseCategoriesLast30Days(Long accountId) {
        LocalDate today = LocalDate.now();
        Date startDate = Date.valueOf(today.minusDays(LOOKBACK_DAYS - 1L));
        Date endDate = Date.valueOf(today);

        List<Object[]> rows = transactionRepository.sumExpenseByCategory(accountId, startDate, endDate);
        List<CategorySpend> topCategories = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 3) {
                continue;
            }
            String categoryName = parseCategoryName(row[1]);
            BigDecimal monthlyExpense = normalizeNonNegative(parseMoney(row[2]));
            if (monthlyExpense.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            topCategories.add(new CategorySpend(categoryName, monthlyExpense));
            if (topCategories.size() >= TOP_EXPENSE_CATEGORY_LIMIT) {
                break;
            }
        }
        return topCategories;
    }

    private BigDecimal computeFeasibilityScore(BigDecimal value, BigDecimal required) {
        BigDecimal requiredSafe = normalizeNonNegative(required);
        if (requiredSafe.compareTo(BigDecimal.ZERO) == 0) {
            return HUNDRED;
        }
        BigDecimal raw = normalizeNonNegative(value)
                .multiply(new BigDecimal("100"))
                .divide(requiredSafe, 2, RoundingMode.HALF_UP);
        if (raw.compareTo(HUNDRED) > 0) {
            return HUNDRED;
        }
        return raw;
    }

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private long toCents(BigDecimal amount) {
        return normalizeNonNegative(amount).movePointRight(2).longValueExact();
    }

    private BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentageAmount(BigDecimal amount, int percent) {
        return normalizeNonNegative(amount)
                .multiply(BigDecimal.valueOf(percent))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseMoney(Object raw) {
        if (raw == null) {
            return ZERO;
        }
        if (raw instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(raw.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return ZERO;
        }
    }

    private String parseCategoryName(Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return "Uncategorized";
        }
        return raw.toString().trim();
    }

    private String formatAmount(BigDecimal amount) {
        return normalizeNonNegative(amount).toPlainString();
    }

    private static class AllocationState {
        private final SavingTarget target;
        private final long requiredCents;
        private final int weight;
        private final String notes;
        private final Goal.Priority priority;
        private long allocatedCents;
        private double fractionalRemainder;

        private AllocationState(SavingTarget target, long requiredCents, int weight, String notes, Goal.Priority priority) {
            this.target = target;
            this.requiredCents = Math.max(0L, requiredCents);
            this.weight = Math.max(1, weight);
            this.notes = notes;
            this.priority = priority == null ? Goal.Priority.MEDIUM : priority;
            this.allocatedCents = 0L;
            this.fractionalRemainder = 0d;
        }

        public SavingTarget getTarget() { return target; }
        public long getRequiredCents() { return requiredCents; }
        public int getWeight() { return weight; }
        public String getNotes() { return notes; }
        public Goal.Priority getPriority() { return priority; }
        public long getAllocatedCents() { return allocatedCents; }
        public void setAllocatedCents(long allocatedCents) { this.allocatedCents = Math.max(0L, allocatedCents); }
        public double getFractionalRemainder() { return fractionalRemainder; }
        public void setFractionalRemainder(double fractionalRemainder) { this.fractionalRemainder = fractionalRemainder; }
    }

    private record CategorySpend(String categoryName, BigDecimal monthlyExpense) {}
}
