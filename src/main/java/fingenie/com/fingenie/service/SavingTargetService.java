package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.SavingTarget;
import fingenie.com.fingenie.entity.Goal;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.PiggyBankMember;
import fingenie.com.fingenie.entity.SavingContribution;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyGoalRepository;
import fingenie.com.fingenie.repository.SavingContributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingTargetService {

    private static final int DEFAULT_HORIZON_MONTHS = 6;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PiggyGoalRepository piggyGoalRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;
    private final SavingContributionRepository savingContributionRepository;
    private final PiggyShareService piggyShareService;

    public List<SavingTarget> getUserSavingTargets(Long accountId) {
        List<SavingTarget> targets = new ArrayList<>();
        targets.addAll(buildGoalTargets(accountId));
        targets.addAll(buildPiggyTargets(accountId));
        return targets;
    }

    private List<SavingTarget> buildGoalTargets(Long accountId) {
        List<Goal> goals = piggyGoalRepository.findByAccountIdOrderByDeadlineAsc(accountId);
        List<SavingTarget> results = new ArrayList<>(goals.size());

        for (Goal goal : goals) {
            BigDecimal targetAmount = normalize(goal.getTargetAmount());
            BigDecimal currentAmount = normalize(goal.getCurrentAmount());
            BigDecimal remainingAmount = remaining(targetAmount, currentAmount);
            int monthsRemaining = resolveGoalMonthsRemaining(goal.getDeadline());
            BigDecimal requiredMonthly = divideMonthly(remainingAmount, monthsRemaining);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("completed", goal.isCompleted());
            if (goal.getIconUrl() != null && !goal.getIconUrl().isBlank()) {
                metadata.put("iconUrl", goal.getIconUrl());
            }

            results.add(SavingTarget.builder()
                    .type(SavingTarget.TargetType.GOAL)
                    .id(goal.getId())
                    .title(goal.getTitle())
                    .targetAmount(targetAmount)
                    .currentAmount(currentAmount)
                    .remainingAmount(remainingAmount)
                    .deadline(goal.getDeadline())
                    .requiredMonthly(requiredMonthly)
                    .notes("Goal target")
                    .metadata(metadata)
                    .build());
        }

        return results;
    }

    private List<SavingTarget> buildPiggyTargets(Long accountId) {
        List<PiggyBankMember> members = piggyBankMemberRepository.findByAccountId(accountId);
        Map<Long, PiggyBankMember> uniqueMemberByPiggyId = new LinkedHashMap<>();

        for (PiggyBankMember member : members) {
            if (member.getPiggyBank() == null || member.getPiggyBank().getId() == null) {
                continue;
            }
            uniqueMemberByPiggyId.putIfAbsent(member.getPiggyBank().getId(), member);
        }

        List<SavingTarget> results = new ArrayList<>(uniqueMemberByPiggyId.size());
        for (PiggyBankMember member : uniqueMemberByPiggyId.values()) {
            Long piggyId = member.getPiggyBank().getId();
            PiggyBank piggyBank = member.getPiggyBank();

            BigDecimal memberTargetAmount = piggyShareService.getMemberTargetAmount(piggyId, accountId);
            BigDecimal memberContributedAmount = normalize(
                    savingContributionRepository.sumAmountByTargetTypeAndTargetIdAndAccountId(
                            SavingContribution.TargetType.PIGGY,
                            piggyId,
                            accountId
                    )
            );
            BigDecimal remainingAmount = remaining(memberTargetAmount, memberContributedAmount);

            BigDecimal sharePercent = piggyShareService.getMemberSharePercent(piggyId, accountId);
            BigDecimal requiredMonthly = piggyShareService.getMemberRequiredMonthly(piggyId, accountId);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sharePercent", sharePercent);
            metadata.put("shareWeight", member.getShareWeight());
            metadata.put("role", member.getRole().name());
            metadata.put("isShared", piggyBank.isShared());

            results.add(SavingTarget.builder()
                    .type(SavingTarget.TargetType.PIGGY)
                    .id(piggyId)
                    .title(resolvePiggyTitle(piggyBank))
                    .targetAmount(memberTargetAmount)
                    .currentAmount(memberContributedAmount)
                    .remainingAmount(remainingAmount)
                    .deadline(toInstant(piggyBank.getLockUntil()))
                    .requiredMonthly(requiredMonthly)
                    .notes("Piggy member target")
                    .metadata(metadata)
                    .build());
        }

        return results;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal remaining(BigDecimal targetAmount, BigDecimal currentAmount) {
        BigDecimal remaining = normalize(targetAmount).subtract(normalize(currentAmount));
        return remaining.compareTo(ZERO) > 0 ? remaining : ZERO;
    }

    private BigDecimal divideMonthly(BigDecimal remainingAmount, int monthsRemaining) {
        if (remainingAmount == null || remainingAmount.compareTo(ZERO) <= 0) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return remainingAmount.divide(BigDecimal.valueOf(Math.max(1, monthsRemaining)), 2, RoundingMode.CEILING);
    }

    private int resolveGoalMonthsRemaining(Instant deadline) {
        if (deadline == null) {
            return DEFAULT_HORIZON_MONTHS;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate dueDate = deadline.atZone(ZoneOffset.UTC).toLocalDate();
        long days = ChronoUnit.DAYS.between(today, dueDate);
        long months = (long) Math.ceil(days / 30.0d);
        return (int) Math.max(1L, months);
    }

    private Instant toInstant(Date date) {
        if (date == null) {
            return null;
        }
        return date.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private String resolvePiggyTitle(PiggyBank piggyBank) {
        if (piggyBank == null || piggyBank.getId() == null) {
            return "Piggy";
        }
        if (piggyBank.getWallet() != null
                && piggyBank.getWallet().getWalletName() != null
                && !piggyBank.getWallet().getWalletName().isBlank()) {
            return piggyBank.getWallet().getWalletName();
        }
        return "Piggy #" + piggyBank.getId();
    }
}
