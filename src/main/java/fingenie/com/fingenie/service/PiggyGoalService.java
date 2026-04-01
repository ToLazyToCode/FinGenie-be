package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.PiggyGoalRequest;
import fingenie.com.fingenie.dto.PiggyGoalResponse;
import fingenie.com.fingenie.dto.PiggyGoalResponse.DepositResult;
import fingenie.com.fingenie.dto.PiggyGoalResponse.PiggyGoalSummary;
import fingenie.com.fingenie.entity.Goal;
import fingenie.com.fingenie.event.PiggyGoalDepositedEvent;
import fingenie.com.fingenie.repository.PiggyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PiggyGoalService {

    private final PiggyGoalRepository repository;
    private final ApplicationEventPublisher events;
    
    private static final int XP_PER_DEPOSIT = 10;
    private static final int XP_GOAL_COMPLETE = 100;

    public PiggyGoalResponse createGoal(Long accountId, PiggyGoalRequest req) {
        Goal goal = Goal.builder()
                .accountId(accountId)
                .title(req.getTitle())
                .iconUrl(req.getIconUrl())
                .targetAmount(req.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .deadline(req.getDeadline())
                .isCompleted(false)
                .build();
        Goal saved = repository.save(goal);
        return toResponse(saved);
    }

    public List<PiggyGoalResponse> listGoals(Long accountId) {
        return repository.findByAccountIdOrderByDeadlineAsc(accountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Internal method to get raw entities for AI context building.
     */
    public List<Goal> listGoalEntities(Long accountId) {
        return repository.findByAccountIdOrderByDeadlineAsc(accountId);
    }
    
    public PiggyGoalSummary getGoalSummary(Long accountId) {
        List<Goal> goals = repository.findByAccountId(accountId);
        List<PiggyGoalResponse> goalResponses = goals.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        BigDecimal totalSaved = repository.getTotalSaved(accountId);
        BigDecimal totalTargeted = repository.getTotalTargeted(accountId);
        long completedCount = repository.countCompletedGoals(accountId);
        
        int overallProgress = 0;
        if (totalTargeted.compareTo(BigDecimal.ZERO) > 0) {
            overallProgress = totalSaved.multiply(BigDecimal.valueOf(100))
                    .divide(totalTargeted, 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        
        return PiggyGoalSummary.builder()
                .totalGoals(goals.size())
                .completedGoals((int) completedCount)
                .activeGoals(goals.size() - (int) completedCount)
                .totalSaved(totalSaved)
                .totalTargeted(totalTargeted)
                .overallProgressPercent(Math.min(100, overallProgress))
                .goals(goalResponses)
                .build();
    }
    
    public PiggyGoalResponse getGoalById(Long accountId, Long goalId) {
        Goal goal = repository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        return toResponse(goal);
    }

    @Transactional
    public DepositResult deposit(Long accountId, Long goalId, BigDecimal amount) {
        Goal goal = repository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        boolean wasCompletedBefore = goal.checkCompleted();
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        boolean isCompletedNow = goal.checkCompleted();
        
        boolean goalJustCompleted = !wasCompletedBefore && isCompletedNow;
        int xpAwarded = XP_PER_DEPOSIT;
        String achievementUnlocked = null;
        
        if (goalJustCompleted) {
            goal.setCompleted(true);
            goal.setCompletedAt(Instant.now());
            xpAwarded += XP_GOAL_COMPLETE;
            
            long completedCount = repository.countCompletedGoals(accountId) + 1;
            if (completedCount == 1) {
                achievementUnlocked = "FIRST_GOAL_COMPLETE";
            } else if (completedCount == 5) {
                achievementUnlocked = "GOAL_CRUSHER";
            } else if (completedCount == 10) {
                achievementUnlocked = "SAVINGS_MASTER";
            }
        }
        
        Goal saved = repository.save(goal);
        events.publishEvent(new PiggyGoalDepositedEvent(this, accountId, goalId, amount));
        
        return DepositResult.builder()
                .goal(toResponse(saved))
                .depositedAmount(amount)
                .goalJustCompleted(goalJustCompleted)
                .xpAwarded(xpAwarded)
                .achievementUnlocked(achievementUnlocked)
                .build();
    }
    
    @Transactional
    public DepositResult withdraw(Long accountId, Long goalId, BigDecimal amount) {
        Goal goal = repository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (goal.getCurrentAmount().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds in savings goal");
        }
        
        goal.setCurrentAmount(goal.getCurrentAmount().subtract(amount));
        
        // Revert completion status if applicable
        if (goal.isCompleted() && !goal.checkCompleted()) {
            goal.setCompleted(false);
            goal.setCompletedAt(null);
        }
        
        Goal saved = repository.save(goal);
        
        return DepositResult.builder()
                .goal(toResponse(saved))
                .depositedAmount(amount.negate())
                .goalJustCompleted(false)
                .xpAwarded(0)
                .build();
    }
    
    @Transactional
    public PiggyGoalResponse updateGoal(Long accountId, Long goalId, PiggyGoalRequest req) {
        Goal goal = repository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (req.getTitle() != null) goal.setTitle(req.getTitle());
        if (req.getIconUrl() != null) goal.setIconUrl(req.getIconUrl());
        if (req.getTargetAmount() != null) goal.setTargetAmount(req.getTargetAmount());
        if (req.getDeadline() != null) goal.setDeadline(req.getDeadline());
        
        Goal saved = repository.save(goal);
        return toResponse(saved);
    }
    
    @Transactional
    public void deleteGoal(Long accountId, Long goalId) {
        Goal goal = repository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        repository.delete(goal);
    }
    
    private PiggyGoalResponse toResponse(Goal goal) {
        long daysRemaining = 0;
        boolean isOverdue = false;
        BigDecimal suggestedDaily = BigDecimal.ZERO;
        
        if (goal.getDeadline() != null) {
            Duration duration = Duration.between(Instant.now(), goal.getDeadline());
            daysRemaining = duration.toDays();
            isOverdue = daysRemaining < 0 && !goal.checkCompleted();
            
            if (daysRemaining > 0 && goal.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                suggestedDaily = goal.getRemainingAmount()
                        .divide(BigDecimal.valueOf(daysRemaining), 2, RoundingMode.CEILING);
            }
        }
        
        Instant createdAt = goal.getCreatedAt() != null ? goal.getCreatedAt().toInstant() : null;
        Instant updatedAt = goal.getUpdatedAt() != null ? goal.getUpdatedAt().toInstant() : null;
        
        return PiggyGoalResponse.builder()
                .id(goal.getId())
                .accountId(goal.getAccountId())
                .title(goal.getTitle())
                .iconUrl(goal.getIconUrl())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .remainingAmount(goal.getRemainingAmount())
                .progressPercent(goal.getProgressPercent())
                .deadline(goal.getDeadline())
                .daysRemaining(daysRemaining)
                .isCompleted(goal.checkCompleted())
                .isOverdue(isOverdue)
                .suggestedDailyAmount(suggestedDaily)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
