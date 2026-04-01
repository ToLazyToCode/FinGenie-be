package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.AuthenticationExceptions;
import fingenie.com.fingenie.common.error.exceptions.ResourceExceptions;
import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.config.CreationGuardrailConfig;
import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.PiggyBankMember;
import fingenie.com.fingenie.entity.Wallet;
import fingenie.com.fingenie.dto.PiggyBankRequest;
import fingenie.com.fingenie.dto.PiggyBankResponse;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import fingenie.com.fingenie.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PiggyBankService {

    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;
    private final WalletRepository walletRepository;
    private final PiggyAuthorizationService piggyAuthorizationService;
    private final CreationGuardrailConfig guardrailConfig;
    private final CreationThrottleService creationThrottleService;

    @Transactional
    public PiggyBankResponse create(PiggyBankRequest request) {
        Account account = SecurityUtils.getCurrentAccount();
        Long accountId = account.getId();

        creationThrottleService.assertPiggyCreateAllowed(accountId);

        long ownedPiggyCount = piggyBankRepository.countByWalletAccountId(accountId);
        int maxPerAccount = Math.max(guardrailConfig.getPiggy().getMaxPerAccount(), 1);
        if (ownedPiggyCount >= maxPerAccount) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "piggyMaxPerAccount", maxPerAccount,
                    "ownedPiggyCount", ownedPiggyCount
            ));
        }

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResourceExceptions.WalletNotFoundException(request.getWalletId()));

        if (!wallet.getAccount().getId().equals(accountId)) {
            throw new AuthenticationExceptions.AccessDeniedException("piggy-bank-create");
        }

        if (piggyBankRepository.existsByWalletId(wallet.getId())) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "walletId", wallet.getId(),
                    "reason", "piggyExists"
            ));
        }

        PiggyBank piggyBank = PiggyBank.builder()
                .wallet(wallet)
                .goalAmount(request.getGoalAmount())
                .lockUntil(request.getLockUntil())
                .interestRate(request.getInterestRate())
                .withdrawalPenaltyRate(request.getWithdrawalPenaltyRate())
                .isShared(request.isShared())
                .build();

        piggyBank = piggyBankRepository.save(piggyBank);

        ensureOwnerMember(piggyBank, account);

        return mapToResponse(piggyBank);
    }

    @Transactional(readOnly = true)
    public PiggyBankResponse getById(Long piggyId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        if (!piggyAuthorizationService.canViewPiggy(piggyId, accountId)) {
            throw new AuthenticationExceptions.AccessDeniedException("piggy-bank-view");
        }

        PiggyBank piggyBank = piggyBankRepository.findById(piggyId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of("piggyId", "notFound")));
        
        return mapToResponse(piggyBank);
    }

    @Transactional(readOnly = true)
    public List<PiggyBankResponse> getAll(int page, int size) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);

        return piggyBankRepository.findAccessibleByAccountId(
                        accountId,
                        PageRequest.of(normalizedPage, normalizedSize)
                ).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PiggyBankResponse update(Long piggyId, PiggyBankRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        PiggyBank piggyBank = piggyBankRepository.findById(piggyId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of("piggyId", "notFound")));

        if (!piggyBank.getWallet().getAccount().getId().equals(accountId)) {
            throw new AuthenticationExceptions.AccessDeniedException("piggy-bank-update");
        }

        piggyBank.setGoalAmount(request.getGoalAmount());
        piggyBank.setLockUntil(request.getLockUntil());
        piggyBank.setInterestRate(request.getInterestRate());
        piggyBank.setWithdrawalPenaltyRate(request.getWithdrawalPenaltyRate());
        piggyBank.setShared(request.isShared());

        piggyBank = piggyBankRepository.save(piggyBank);

        ensureOwnerMember(piggyBank, piggyBank.getWallet().getAccount());

        return mapToResponse(piggyBank);
    }

    @Transactional
    public void delete(Long piggyId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        PiggyBank piggyBank = piggyBankRepository.findById(piggyId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of("piggyId", "notFound")));

        if (!piggyBank.getWallet().getAccount().getId().equals(accountId)) {
            throw new AuthenticationExceptions.AccessDeniedException("piggy-bank-delete");
        }

        piggyBankRepository.delete(piggyBank);
    }

    private void ensureOwnerMember(PiggyBank piggyBank, Account ownerAccount) {
        if (piggyBankMemberRepository.existsByPiggyBankIdAndAccountId(piggyBank.getId(), ownerAccount.getId())) {
            return;
        }

        PiggyBankMember ownerMember = PiggyBankMember.builder()
                .piggyBank(piggyBank)
                .account(ownerAccount)
                .role(PiggyBankMember.MemberRole.OWNER)
                .build();
        piggyBankMemberRepository.save(ownerMember);
    }

    private PiggyBankResponse mapToResponse(PiggyBank piggyBank) {
        return PiggyBankResponse.builder()
        .piggyId(piggyBank.getId())
                .walletId(piggyBank.getWallet().getId())
                .goalAmount(piggyBank.getGoalAmount())
                .lockUntil(piggyBank.getLockUntil())
                .interestRate(piggyBank.getInterestRate())
                .withdrawalPenaltyRate(piggyBank.getWithdrawalPenaltyRate())
                .isShared(piggyBank.isShared())
                .createdAt(piggyBank.getCreatedAt())
                .build();
    }
}
