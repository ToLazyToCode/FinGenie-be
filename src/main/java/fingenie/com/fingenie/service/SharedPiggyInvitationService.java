package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.CreateSharedPiggyInvitationRequest;
import fingenie.com.fingenie.dto.SharedPiggyInvitationResponse;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.PiggyBankMember;
import fingenie.com.fingenie.entity.SharedPiggyInvitation;
import fingenie.com.fingenie.entity.Wallet;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.FriendshipRepository;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import fingenie.com.fingenie.repository.SharedPiggyInvitationRepository;
import fingenie.com.fingenie.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SharedPiggyInvitationService {

    private static final long INVITATION_EXPIRY_DAYS = 5L;

    private final SharedPiggyInvitationRepository invitationRepository;
    private final AccountRepository accountRepository;
    private final FriendshipRepository friendshipRepository;
    private final WalletRepository walletRepository;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;
    private final NotificationService notificationService;

    @Transactional
    public SharedPiggyInvitationResponse createInvitation(Long inviterId, CreateSharedPiggyInvitationRequest request) {
        expirePendingInvitations();

        if (request.getInviteeId().equals(inviterId)) {
            throw new IllegalArgumentException("You cannot invite yourself to a shared piggy");
        }

        if (!friendshipRepository.areFriends(inviterId, request.getInviteeId())) {
            throw new IllegalArgumentException("You can only invite friends to a shared piggy");
        }

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getAccount().getId().equals(inviterId)) {
            throw new SecurityException("You cannot use another user's wallet for a shared piggy");
        }

        if (piggyBankRepository.existsByWalletId(wallet.getId())) {
            throw new IllegalStateException("This wallet already has a piggy bank");
        }

        if (invitationRepository.existsByWalletIdAndStatus(wallet.getId(), SharedPiggyInvitation.Status.PENDING)) {
            throw new IllegalStateException("A shared piggy invitation is already pending for this wallet");
        }

        if (invitationRepository.existsByWalletIdAndInviteeIdAndStatus(
                wallet.getId(),
                request.getInviteeId(),
                SharedPiggyInvitation.Status.PENDING
        )) {
            throw new IllegalStateException("You already sent this friend a pending invitation for the same wallet");
        }

        Account inviter = accountRepository.findById(inviterId)
                .orElseThrow(() -> new IllegalArgumentException("Inviter account not found"));
        Account invitee = accountRepository.findById(request.getInviteeId())
                .orElseThrow(() -> new IllegalArgumentException("Invitee account not found"));

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(INVITATION_EXPIRY_DAYS);
        SharedPiggyInvitation invitation = SharedPiggyInvitation.builder()
                .inviterId(inviterId)
                .inviteeId(invitee.getId())
                .walletId(wallet.getId())
                .piggyTitle(normalizeTitle(request.getPiggyTitle()))
                .goalAmount(request.getGoalAmount())
                .lockUntil(request.getLockUntil())
                .status(SharedPiggyInvitation.Status.PENDING)
                .expiresAt(expiresAt)
                .build();

        invitation = invitationRepository.save(invitation);

        notificationService.createNotification(
                invitee.getId(),
                NotificationService.TYPE_SHARED_PIGGY_INVITATION,
                "Shared piggy invitation",
                inviter.getName() + " invited you to join \"" + invitation.getPiggyTitle() + "\".",
                NotificationService.ACTION_DISMISS,
                null,
                2,
                expiresAt
        );

        log.info("Created shared piggy invitation {} from account {} to {}", invitation.getId(), inviterId, invitee.getId());
        return toResponse(invitation, Map.of(inviter.getId(), inviter, invitee.getId(), invitee));
    }

    @Transactional
    public List<SharedPiggyInvitationResponse> getIncomingInvitations(Long accountId) {
        expirePendingInvitations();

        List<SharedPiggyInvitation> invitations = invitationRepository.findByInviteeIdAndStatusOrderByCreatedAtDesc(
                accountId,
                SharedPiggyInvitation.Status.PENDING
        ).stream()
                .filter(invitation -> invitation.getExpiresAt() != null && invitation.getExpiresAt().isAfter(LocalDateTime.now()))
                .toList();

        return mapResponses(invitations);
    }

    @Transactional
    public SharedPiggyInvitationResponse acceptInvitation(Long inviteeId, Long invitationId) {
        expirePendingInvitations();

        SharedPiggyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, inviteeId)
                .orElseThrow(() -> new IllegalArgumentException("Shared piggy invitation not found"));
        assertPending(invitation);

        Account inviter = accountRepository.findById(invitation.getInviterId())
                .orElseThrow(() -> new IllegalArgumentException("Inviter account not found"));
        Account invitee = accountRepository.findById(inviteeId)
                .orElseThrow(() -> new IllegalArgumentException("Invitee account not found"));
        Wallet wallet = walletRepository.findById(invitation.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getAccount().getId().equals(inviter.getId())) {
            throw new IllegalStateException("Invitation wallet owner mismatch");
        }
        if (piggyBankRepository.existsByWalletId(wallet.getId())) {
            throw new IllegalStateException("This shared piggy has already been created");
        }

        PiggyBank piggyBank = PiggyBank.builder()
                .wallet(wallet)
                .goalAmount(invitation.getGoalAmount())
                .lockUntil(invitation.getLockUntil())
                .interestRate(null)
                .withdrawalPenaltyRate(null)
                .isShared(true)
                .build();
        piggyBank = piggyBankRepository.save(piggyBank);

        piggyBankMemberRepository.save(PiggyBankMember.builder()
                .piggyBank(piggyBank)
                .account(inviter)
                .role(PiggyBankMember.MemberRole.OWNER)
                .shareWeight(1)
                .monthlyCommitment(BigDecimal.ZERO)
                .build());

        piggyBankMemberRepository.save(PiggyBankMember.builder()
                .piggyBank(piggyBank)
                .account(invitee)
                .role(PiggyBankMember.MemberRole.CONTRIBUTOR)
                .shareWeight(1)
                .monthlyCommitment(BigDecimal.ZERO)
                .build());

        invitation.setStatus(SharedPiggyInvitation.Status.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setRespondedAt(invitation.getAcceptedAt());
        invitation.setCreatedPiggyId(piggyBank.getId());
        invitation = invitationRepository.save(invitation);

        notificationService.createSimpleNotification(
                inviter.getId(),
                NotificationService.TYPE_SHARED_PIGGY_INVITATION,
                "Shared piggy accepted",
                invitee.getName() + " accepted your shared piggy invitation."
        );

        log.info("Accepted shared piggy invitation {} and created piggy {}", invitationId, piggyBank.getId());
        return toResponse(invitation, Map.of(inviter.getId(), inviter, invitee.getId(), invitee));
    }

    @Transactional
    public SharedPiggyInvitationResponse rejectInvitation(Long inviteeId, Long invitationId) {
        expirePendingInvitations();

        SharedPiggyInvitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, inviteeId)
                .orElseThrow(() -> new IllegalArgumentException("Shared piggy invitation not found"));
        assertPending(invitation);

        invitation.setStatus(SharedPiggyInvitation.Status.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);

        Account inviter = accountRepository.findById(invitation.getInviterId())
                .orElseThrow(() -> new IllegalArgumentException("Inviter account not found"));
        Account invitee = accountRepository.findById(inviteeId)
                .orElseThrow(() -> new IllegalArgumentException("Invitee account not found"));

        notificationService.createSimpleNotification(
                inviter.getId(),
                NotificationService.TYPE_SHARED_PIGGY_INVITATION,
                "Shared piggy declined",
                invitee.getName() + " declined your shared piggy invitation."
        );

        log.info("Rejected shared piggy invitation {}", invitationId);
        return toResponse(invitation, Map.of(inviter.getId(), inviter, invitee.getId(), invitee));
    }

    @Transactional
    public int expirePendingInvitations() {
        return invitationRepository.expirePendingInvitations(
                SharedPiggyInvitation.Status.PENDING,
                SharedPiggyInvitation.Status.EXPIRED,
                LocalDateTime.now()
        );
    }

    private List<SharedPiggyInvitationResponse> mapResponses(List<SharedPiggyInvitation> invitations) {
        if (invitations.isEmpty()) {
            return List.of();
        }

        List<Long> accountIds = invitations.stream()
                .flatMap(invitation -> java.util.stream.Stream.of(invitation.getInviterId(), invitation.getInviteeId()))
                .distinct()
                .toList();
        Map<Long, Account> accounts = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        return invitations.stream()
                .map(invitation -> toResponse(invitation, accounts))
                .toList();
    }

    private SharedPiggyInvitationResponse toResponse(SharedPiggyInvitation invitation, Map<Long, Account> accounts) {
        Account inviter = accounts.get(invitation.getInviterId());
        Account invitee = accounts.get(invitation.getInviteeId());

        return SharedPiggyInvitationResponse.builder()
                .id(invitation.getId())
                .inviterId(invitation.getInviterId())
                .inviterName(inviter != null ? inviter.getName() : "Unknown")
                .inviterAvatar(inviter != null ? inviter.getAvatarUrl() : null)
                .inviteeId(invitation.getInviteeId())
                .inviteeName(invitee != null ? invitee.getName() : "Unknown")
                .inviteeAvatar(invitee != null ? invitee.getAvatarUrl() : null)
                .walletId(invitation.getWalletId())
                .piggyTitle(invitation.getPiggyTitle())
                .goalAmount(invitation.getGoalAmount())
                .lockUntil(invitation.getLockUntil())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .respondedAt(invitation.getRespondedAt())
                .acceptedAt(invitation.getAcceptedAt())
                .createdPiggyId(invitation.getCreatedPiggyId())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    private void assertPending(SharedPiggyInvitation invitation) {
        if (invitation.getStatus() != SharedPiggyInvitation.Status.PENDING) {
            throw new IllegalStateException("This shared piggy invitation is no longer pending");
        }
        if (invitation.getExpiresAt() == null || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(SharedPiggyInvitation.Status.EXPIRED);
            invitation.setRespondedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
            throw new IllegalStateException("This shared piggy invitation has expired");
        }
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            return "Shared Piggy";
        }
        return normalized;
    }
}


