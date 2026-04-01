package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.Goal;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.SavingContribution;
import fingenie.com.fingenie.entity.Wallet;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import fingenie.com.fingenie.repository.PiggyGoalRepository;
import fingenie.com.fingenie.repository.SavingContributionRepository;
import fingenie.com.fingenie.repository.WalletRepository;
import fingenie.com.fingenie.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingContributionService {

    private final SavingContributionRepository savingContributionRepository;
    private final PiggyGoalRepository piggyGoalRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;
    private final WalletRepository walletRepository;
    private final EntitlementService entitlementService;

    @Transactional
    public SavingContribution addGoalContribution(Long goalId, BigDecimal amount, SavingContribution.Source source) {
        validateAmount(amount);
        Long accountId = SecurityUtils.getCurrentAccountId();
        enforceAdvancedSavingsRule(accountId, source);

        Goal goal = piggyGoalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!goal.getAccountId().equals(accountId)) {
            throw new RuntimeException("Only goal owner can contribute");
        }

        SavingContribution contribution = saveContribution(
                accountId,
                SavingContribution.TargetType.GOAL,
                goalId,
                amount,
                source
        );

        BigDecimal currentAmount = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
        goal.setCurrentAmount(currentAmount.add(amount));
        if (goal.checkCompleted()) {
            goal.setCompleted(true);
            if (goal.getCompletedAt() == null) {
                goal.setCompletedAt(Instant.now());
            }
        }
        piggyGoalRepository.save(goal);

        return contribution;
    }

    @Transactional
    public SavingContribution addPiggyContribution(Long piggyId, BigDecimal amount, SavingContribution.Source source) {
        validateAmount(amount);
        Long accountId = SecurityUtils.getCurrentAccountId();
        enforceAdvancedSavingsRule(accountId, source);

        PiggyBank piggyBank = piggyBankRepository.findById(piggyId)
                .orElseThrow(() -> new RuntimeException("Piggy bank not found"));

        if (!piggyBankMemberRepository.existsByPiggyBankIdAndAccountId(piggyId, accountId)) {
            throw new RuntimeException("Only piggy members can contribute");
        }

        SavingContribution contribution = saveContribution(
                accountId,
                SavingContribution.TargetType.PIGGY,
                piggyId,
                amount,
                source
        );

        Wallet wallet = piggyBank.getWallet();
        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        wallet.setBalance(balance.add(amount));
        walletRepository.save(wallet);

        return contribution;
    }

    @Transactional(readOnly = true)
    public BigDecimal getMemberPiggyContributedSum(Long piggyId, Long accountId) {
        return savingContributionRepository.sumAmountByTargetTypeAndTargetIdAndAccountId(
                SavingContribution.TargetType.PIGGY,
                piggyId,
                accountId
        );
    }

    @Transactional(readOnly = true)
    public List<SavingContribution> getRecentContributionsForCurrentAccount(int limit) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        int normalizedLimit = normalizeLimit(limit);
        return savingContributionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, normalizedLimit))
                .getContent();
    }

    private SavingContribution saveContribution(
            Long accountId,
            SavingContribution.TargetType targetType,
            Long targetId,
            BigDecimal amount,
            SavingContribution.Source source
    ) {
        SavingContribution contribution = SavingContribution.builder()
                .accountId(accountId)
                .targetType(targetType)
                .targetId(targetId)
                .amount(amount)
                .source(source == null ? SavingContribution.Source.MANUAL : source)
                .build();
        return savingContributionRepository.save(contribution);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Contribution amount must be greater than 0");
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 500);
    }

    private void enforceAdvancedSavingsRule(Long accountId, SavingContribution.Source source) {
        if (source == SavingContribution.Source.AUTO) {
            entitlementService.assertFeature(accountId, "savings.suggest.advanced");
        }
    }
}
