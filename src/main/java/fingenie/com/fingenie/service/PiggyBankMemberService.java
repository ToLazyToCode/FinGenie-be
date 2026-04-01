package fingenie.com.fingenie.service;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.PiggyBankMember;
import fingenie.com.fingenie.dto.PiggyBankMemberRequest;
import fingenie.com.fingenie.dto.PiggyBankMemberResponse;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PiggyBankMemberService {

    private final PiggyBankMemberRepository piggyBankMemberRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final AccountRepository accountRepository;
    private final PiggyAuthorizationService piggyAuthorizationService;

    @Transactional
    public PiggyBankMemberResponse create(PiggyBankMemberRequest request) {
        Account currentAccount = SecurityUtils.getCurrentAccount();
        PiggyBank piggyBank = piggyBankRepository.findById(request.getPiggyId())
                .orElseThrow(() -> new RuntimeException("Piggy bank not found"));

        // Only owner can add members
        if (!piggyBank.getWallet().getAccount().getId().equals(currentAccount.getId())) {
            throw new RuntimeException("Only owner can add members");
        }

        if (!piggyBank.isShared()) {
            throw new RuntimeException("Piggy bank is not shared");
        }

        Account memberAccount = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if member already exists
        if (piggyBankMemberRepository.existsByPiggyBankIdAndAccountId(request.getPiggyId(), request.getAccountId())) {
            throw new RuntimeException("Member already exists");
        }

        PiggyBankMember member = PiggyBankMember.builder()
                .piggyBank(piggyBank)
                .account(memberAccount)
                .role(request.getRole() != null ? request.getRole() : PiggyBankMember.MemberRole.CONTRIBUTOR)
                .shareWeight(request.getShareWeight() != null ? request.getShareWeight() : 1)
                .monthlyCommitment(request.getMonthlyCommitment() != null ? request.getMonthlyCommitment() : BigDecimal.ZERO)
                .build();

        member = piggyBankMemberRepository.save(member);
        return mapToResponse(member);
    }

    /**
     * Get member by ID.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public PiggyBankMemberResponse getById(Long id) {
        Long currentAccountId = SecurityUtils.getCurrentAccountId();
        PiggyBankMember member = piggyBankMemberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!piggyAuthorizationService.canViewPiggy(member.getPiggyBank().getId(), currentAccountId)) {
            throw new RuntimeException("Not authorized to view this piggy bank member");
        }
        
        return mapToResponse(member);
    }

    /**
     * Get all members for a piggy bank.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<PiggyBankMemberResponse> getByPiggyBank(Long piggyId) {
        Long currentAccountId = SecurityUtils.getCurrentAccountId();
        if (!piggyBankRepository.existsById(piggyId)) {
            throw new RuntimeException("Piggy bank not found");
        }
        if (!piggyAuthorizationService.canViewPiggy(piggyId, currentAccountId)) {
            throw new RuntimeException("Not authorized to view piggy bank members");
        }

        return piggyBankMemberRepository.findByPiggyBankId(piggyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all memberships for current account.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<PiggyBankMemberResponse> getByAccount() {
        Account account = SecurityUtils.getCurrentAccount();
        return piggyBankMemberRepository.findByAccount(account).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PiggyBankMemberResponse updateRole(Long id, PiggyBankMember.MemberRole role) {
        Account currentAccount = SecurityUtils.getCurrentAccount();
        PiggyBankMember member = piggyBankMemberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Only owner can update roles
        if (!member.getPiggyBank().getWallet().getAccount().getId().equals(currentAccount.getId())) {
            throw new RuntimeException("Only owner can update member roles");
        }

        member.setRole(role);
        member = piggyBankMemberRepository.save(member);
        
        return mapToResponse(member);
    }

    @Transactional
    public void delete(Long id) {
        Account currentAccount = SecurityUtils.getCurrentAccount();
        PiggyBankMember member = piggyBankMemberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Only owner can remove members
        if (!member.getPiggyBank().getWallet().getAccount().getId().equals(currentAccount.getId())) {
            throw new RuntimeException("Only owner can remove members");
        }

        piggyBankMemberRepository.delete(member);
    }

    @Transactional
    public PiggyBankMemberResponse updateShareWeight(Long piggyId, Long memberId, Integer shareWeight) {
        if (shareWeight == null || shareWeight < 1) {
            throw new RuntimeException("Share weight must be at least 1");
        }

        Long currentAccountId = SecurityUtils.getCurrentAccountId();
        PiggyBankMember member = piggyBankMemberRepository.findByIdAndPiggyBankId(memberId, piggyId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!isOwnerActor(member.getPiggyBank(), currentAccountId)) {
            throw new RuntimeException("Only owner can update member share weight");
        }

        member.setShareWeight(shareWeight);
        member = piggyBankMemberRepository.save(member);
        return mapToResponse(member);
    }

    @Transactional
    public PiggyBankMemberResponse updateMonthlyCommitment(Long piggyId, Long memberId, BigDecimal monthlyCommitment) {
        if (monthlyCommitment == null || monthlyCommitment.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Monthly commitment must be >= 0");
        }

        Long currentAccountId = SecurityUtils.getCurrentAccountId();
        PiggyBankMember member = piggyBankMemberRepository.findByIdAndPiggyBankId(memberId, piggyId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        boolean isOwner = isOwnerActor(member.getPiggyBank(), currentAccountId);
        boolean isSelf = member.getAccount().getId().equals(currentAccountId);
        if (!isOwner && !isSelf) {
            throw new RuntimeException("Not authorized to update monthly commitment");
        }

        member.setMonthlyCommitment(monthlyCommitment);
        member = piggyBankMemberRepository.save(member);
        return mapToResponse(member);
    }

    private boolean isOwnerActor(PiggyBank piggyBank, Long actorAccountId) {
        if (piggyBank.getWallet().getAccount().getId().equals(actorAccountId)) {
            return true;
        }
        return piggyBankMemberRepository.findByPiggyBankIdAndAccountId(piggyBank.getId(), actorAccountId)
                .map(m -> m.getRole() == PiggyBankMember.MemberRole.OWNER)
                .orElse(false);
    }

    private PiggyBankMemberResponse mapToResponse(PiggyBankMember member) {
        return PiggyBankMemberResponse.builder()
                .id(member.getId())
                .piggyId(member.getPiggyBank().getId())
                .accountId(member.getAccount().getId())
                .accountEmail(member.getAccount().getEmail())
                .role(member.getRole())
                .shareWeight(member.getShareWeight())
                .monthlyCommitment(member.getMonthlyCommitment())
                .joinedAt(member.getCreatedAt())
                .build();
    }
}
