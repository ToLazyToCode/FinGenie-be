package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.PiggyBankMember;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PiggyShareService {

    private static final int DEFAULT_HORIZON_MONTHS = 6;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;

    @Transactional(readOnly = true)
    public BigDecimal getMemberSharePercent(Long piggyId, Long accountId) {
        PiggyBankMember member = piggyBankMemberRepository.findByPiggyBankIdAndAccountId(piggyId, accountId)
                .orElseThrow(() -> new RuntimeException("Piggy bank member not found"));
        Long totalWeight = piggyBankMemberRepository.sumShareWeightByPiggyBankId(piggyId);
        if (totalWeight == null || totalWeight <= 0) {
            totalWeight = (long) Math.max(1, member.getShareWeight());
        }

        return BigDecimal.valueOf(member.getShareWeight())
                .divide(BigDecimal.valueOf(totalWeight), 6, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMemberTargetAmount(Long piggyId, Long accountId) {
        PiggyBank piggyBank = piggyBankRepository.findById(piggyId)
                .orElseThrow(() -> new RuntimeException("Piggy bank not found"));
        BigDecimal sharePercent = getMemberSharePercent(piggyId, accountId);
        return piggyBank.getGoalAmount().multiply(sharePercent).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMemberRequiredMonthly(Long piggyId, Long accountId) {
        PiggyBankMember member = piggyBankMemberRepository.findByPiggyBankIdAndAccountId(piggyId, accountId)
                .orElseThrow(() -> new RuntimeException("Piggy bank member not found"));

        if (member.getMonthlyCommitment() != null && member.getMonthlyCommitment().compareTo(ZERO) > 0) {
            return member.getMonthlyCommitment();
        }

        BigDecimal target = getMemberTargetAmount(piggyId, accountId);
        PiggyBank piggyBank = piggyBankRepository.findById(piggyId)
                .orElseThrow(() -> new RuntimeException("Piggy bank not found"));
        int monthsRemaining = resolveMonthsRemaining(piggyBank);

        return target.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.CEILING);
    }

    private int resolveMonthsRemaining(PiggyBank piggyBank) {
        if (piggyBank.getLockUntil() == null) {
            return DEFAULT_HORIZON_MONTHS;
        }

        LocalDate today = LocalDate.now();
        LocalDate lockDate = piggyBank.getLockUntil().toLocalDate();
        long days = ChronoUnit.DAYS.between(today, lockDate);
        long months = (long) Math.ceil(days / 30.0d);
        return (int) Math.max(1L, months);
    }
}
