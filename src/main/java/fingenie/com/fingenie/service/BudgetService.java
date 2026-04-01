package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.BudgetRequest;
import fingenie.com.fingenie.dto.BudgetResponse;
import fingenie.com.fingenie.dto.BudgetResponse.BudgetAlert;
import fingenie.com.fingenie.dto.BudgetResponse.BudgetStatus;
import fingenie.com.fingenie.dto.BudgetResponse.BudgetSummary;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Budget;
import fingenie.com.fingenie.entity.Budget.PeriodType;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.BudgetRepository;
import fingenie.com.fingenie.repository.CategoryRepository;
import fingenie.com.fingenie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    @Transactional
    public BudgetResponse createBudget(Long accountId, BudgetRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            
            if (budgetRepository.existsByAccountIdAndCategoryIdAndPeriodTypeAndIsActiveTrue(
                    accountId, request.getCategoryId(), request.getPeriodType())) {
                throw new RuntimeException("Budget already exists for this category and period");
            }
        }
        
        Budget budget = Budget.builder()
                .account(account)
                .category(category)
                .amount(request.getAmount())
                .periodType(request.getPeriodType())
                .alertThreshold(request.getAlertThreshold() != null ? request.getAlertThreshold() : 80)
                .isActive(true)
                .notifyOnExceed(request.getNotifyOnExceed() != null ? request.getNotifyOnExceed() : true)
                .notifyOnWarning(request.getNotifyOnWarning() != null ? request.getNotifyOnWarning() : true)
                .rolloverExcess(request.getRolloverExcess() != null ? request.getRolloverExcess() : false)
                .notes(request.getNotes())
                .build();
        
        Budget saved = budgetRepository.save(budget);
        return toResponse(saved, accountId);
    }
    
    /**
     * Get all active budgets for an account.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Long accountId) {
        return budgetRepository.findByAccountIdAndIsActiveTrue(accountId).stream()
                .map(b -> toResponse(b, accountId))
                .collect(Collectors.toList());
    }
    
    /**
     * Get budgets filtered by period type.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByPeriod(Long accountId, PeriodType periodType) {
        return budgetRepository.findByAccountIdAndPeriodTypeAndIsActiveTrue(accountId, periodType).stream()
                .map(b -> toResponse(b, accountId))
                .collect(Collectors.toList());
    }
    
    /**
     * Get comprehensive budget summary for an account.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public BudgetSummary getBudgetSummary(Long accountId) {
        List<Budget> budgets = budgetRepository.findByAccountIdAndIsActiveTrue(accountId);
        
        List<BudgetResponse> responses = budgets.stream()
                .filter(b -> b.getCategory() != null)
                .map(b -> toResponse(b, accountId))
                .collect(Collectors.toList());
        
        Budget totalBudget = budgets.stream()
                .filter(Budget::isTotalBudget)
                .findFirst()
                .orElse(null);
        
        BigDecimal totalBudgetAmount = responses.stream()
                .map(BudgetResponse::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalSpent = responses.stream()
                .map(BudgetResponse::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRemaining = totalBudgetAmount.subtract(totalSpent);
        
        int overallUsage = 0;
        if (totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            overallUsage = totalSpent.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudgetAmount, 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        
        int underControl = (int) responses.stream()
                .filter(r -> r.getStatus() == BudgetStatus.UNDER_BUDGET)
                .count();
        int atWarning = (int) responses.stream()
                .filter(r -> r.getStatus() == BudgetStatus.WARNING || r.getStatus() == BudgetStatus.NEAR_LIMIT)
                .count();
        int exceeded = (int) responses.stream()
                .filter(r -> r.getStatus() == BudgetStatus.EXCEEDED)
                .count();
        
        return BudgetSummary.builder()
                .totalBudget(totalBudgetAmount)
                .totalSpent(totalSpent)
                .totalRemaining(totalRemaining)
                .overallUsagePercent(Math.min(100, overallUsage))
                .budgetsUnderControl(underControl)
                .budgetsAtWarning(atWarning)
                .budgetsExceeded(exceeded)
                .categoryBudgets(responses)
                .totalBudgetInfo(totalBudget != null ? toResponse(totalBudget, accountId) : null)
                .build();
    }
    
    /**
     * Get a single budget by ID.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long accountId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        if (!budget.getAccount().getId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        return toResponse(budget, accountId);
    }
    
    @Transactional
    public BudgetResponse updateBudget(Long accountId, Long budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        if (!budget.getAccount().getId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (request.getAmount() != null) budget.setAmount(request.getAmount());
        if (request.getAlertThreshold() != null) budget.setAlertThreshold(request.getAlertThreshold());
        if (request.getNotifyOnExceed() != null) budget.setNotifyOnExceed(request.getNotifyOnExceed());
        if (request.getNotifyOnWarning() != null) budget.setNotifyOnWarning(request.getNotifyOnWarning());
        if (request.getRolloverExcess() != null) budget.setRolloverExcess(request.getRolloverExcess());
        if (request.getNotes() != null) budget.setNotes(request.getNotes());
        
        Budget saved = budgetRepository.save(budget);
        return toResponse(saved, accountId);
    }
    
    @Transactional
    public void deleteBudget(Long accountId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        if (!budget.getAccount().getId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        budget.setIsActive(false);
        budgetRepository.save(budget);
    }
    
    /**
     * Check for budget alerts.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<BudgetAlert> checkBudgetAlerts(Long accountId) {
        List<Budget> budgets = budgetRepository.findByAccountIdAndIsActiveTrue(accountId);
        List<BudgetAlert> alerts = new ArrayList<>();
        
        for (Budget budget : budgets) {
            BudgetResponse response = toResponse(budget, accountId);
            if (response.getStatus() == BudgetStatus.WARNING || 
                response.getStatus() == BudgetStatus.NEAR_LIMIT ||
                response.getStatus() == BudgetStatus.EXCEEDED) {
                
                BigDecimal overspent = response.getSpentAmount().subtract(response.getBudgetAmount());
                
                alerts.add(BudgetAlert.builder()
                        .budgetId(budget.getId())
                        .categoryId(budget.getCategory() != null ? budget.getCategory().getId() : null)
                        .categoryName(budget.getCategory() != null ? budget.getCategory().getCategoryName() : "Total")
                        .status(response.getStatus())
                        .usagePercent(response.getUsagePercent())
                        .overspentBy(overspent.compareTo(BigDecimal.ZERO) > 0 ? overspent : BigDecimal.ZERO)
                        .message(response.getStatusMessage())
                        .timestamp(new Timestamp(System.currentTimeMillis()))
                        .build());
            }
        }
        
        return alerts;
    }
    
    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    @Transactional
    public void sendDailyBudgetAlerts() {
        log.info("Running daily budget alert check...");
        
        List<Budget> warningBudgets = budgetRepository.findAllActiveWithWarningEnabled();
        
        for (Budget budget : warningBudgets) {
            Long accountId = budget.getAccount().getId();
            BudgetResponse response = toResponse(budget, accountId);
            
            if (response.getStatus() == BudgetStatus.WARNING && budget.getNotifyOnWarning()) {
                notificationService.sendBudgetWarning(
                        accountId,
                        response.getCategoryName(),
                        response.getUsagePercent()
                );
            }
            
            if (response.getStatus() == BudgetStatus.EXCEEDED && budget.getNotifyOnExceed()) {
                notificationService.sendBudgetExceeded(
                        accountId,
                        response.getCategoryName(),
                        response.getSpentAmount().subtract(response.getBudgetAmount())
                );
            }
        }
    }
    
    private BudgetResponse toResponse(Budget budget, Long accountId) {
        BigDecimal spent = calculateSpent(accountId, budget);
        BigDecimal remaining = budget.getAmount().subtract(spent);
        int usagePercent = 0;
        
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            usagePercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getAmount(), 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        
        BudgetStatus status = calculateStatus(usagePercent, budget.getAlertThreshold());
        String statusMessage = generateStatusMessage(status, usagePercent, remaining, budget);
        
        return BudgetResponse.builder()
                .id(budget.getId())
                .accountId(accountId)
                .categoryId(budget.getCategory() != null ? budget.getCategory().getId() : null)
                .categoryName(budget.getCategory() != null ? budget.getCategory().getCategoryName() : "Total Budget")
                .categoryIcon(null) // Category doesn't have icon field
                .budgetAmount(budget.getAmount())
                .spentAmount(spent)
                .remainingAmount(remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO)
                .usagePercent(usagePercent)
                .periodType(budget.getPeriodType())
                .alertThreshold(budget.getAlertThreshold())
                .isActive(budget.getIsActive())
                .notifyOnExceed(budget.getNotifyOnExceed())
                .notifyOnWarning(budget.getNotifyOnWarning())
                .rolloverExcess(budget.getRolloverExcess())
                .notes(budget.getNotes())
                .status(status)
                .statusMessage(statusMessage)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
    
    private BigDecimal calculateSpent(Long accountId, Budget budget) {
        LocalDate[] dateRange = getPeriodDateRange(budget.getPeriodType());
        java.sql.Date startDate = java.sql.Date.valueOf(dateRange[0]);
        java.sql.Date endDate = java.sql.Date.valueOf(dateRange[1]);
        
        if (budget.getCategory() != null) {
            return transactionRepository.sumExpensesByAccountAndCategoryBetween(
                    accountId, budget.getCategory().getId(), startDate, endDate);
        } else {
            return transactionRepository.sumExpensesByAccountBetween(accountId, startDate, endDate);
        }
    }
    
    private LocalDate[] getPeriodDateRange(PeriodType periodType) {
        LocalDate today = LocalDate.now();
        LocalDate start, end;
        
        switch (periodType) {
            case DAILY:
                start = today;
                end = today;
                break;
            case WEEKLY:
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
                break;
            case MONTHLY:
                start = today.withDayOfMonth(1);
                end = today.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case YEARLY:
                start = today.withDayOfYear(1);
                end = today.with(TemporalAdjusters.lastDayOfYear());
                break;
            default:
                start = today.withDayOfMonth(1);
                end = today.with(TemporalAdjusters.lastDayOfMonth());
        }
        
        return new LocalDate[]{start, end};
    }
    
    private BudgetStatus calculateStatus(int usagePercent, int alertThreshold) {
        if (usagePercent >= 100) return BudgetStatus.EXCEEDED;
        if (usagePercent >= 95) return BudgetStatus.NEAR_LIMIT;
        if (usagePercent >= alertThreshold) return BudgetStatus.WARNING;
        return BudgetStatus.UNDER_BUDGET;
    }
    
    private String generateStatusMessage(BudgetStatus status, int usagePercent, 
                                         BigDecimal remaining, Budget budget) {
        return switch (status) {
            case UNDER_BUDGET -> String.format("You're on track! %s remaining", formatMoney(remaining));
            case WARNING -> String.format("Careful! You've used %d%% of your budget", usagePercent);
            case NEAR_LIMIT -> String.format("Almost at limit! Only %s left", formatMoney(remaining));
            case EXCEEDED -> String.format("Budget exceeded by %s", formatMoney(remaining.abs()));
            default -> "Budget inactive";
        };
    }
    
    private String formatMoney(BigDecimal amount) {
        return String.format("$%.2f", amount.abs());
    }
}
