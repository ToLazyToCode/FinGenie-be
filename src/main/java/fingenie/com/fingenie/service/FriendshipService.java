package fingenie.com.fingenie.service;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Friendship;
import fingenie.com.fingenie.dto.FriendshipRequest;
import fingenie.com.fingenie.dto.FriendshipResponse;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public FriendshipResponse create(FriendshipRequest request) {
        Account requester = SecurityUtils.getCurrentAccount();
        Account addressee = accountRepository.findById(request.getAddresseeId())
                .orElseThrow(() -> new RuntimeException("Addressee not found"));

        if (requester.getId().equals(addressee.getId())) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }

        // Check if friendship already exists
        friendshipRepository.findFriendshipBetweenAccounts(requester, addressee)
                .ifPresent(f -> {
                    throw new RuntimeException("Friendship already exists");
                });

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(Friendship.FriendshipStatus.PENDING)
                .build();

        friendship = friendshipRepository.save(friendship);
        return mapToResponse(friendship);
    }

    /**
     * Get friendship by ID.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public FriendshipResponse getById(Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));
        
        return mapToResponse(friendship);
    }

    /**
     * Get all friendships for current account.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getAll() {
        Account account = SecurityUtils.getCurrentAccount();
        List<Friendship> friendships = friendshipRepository.findByRequester(account);
        friendships.addAll(friendshipRepository.findByAddressee(account));
        
        return friendships.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get friendships filtered by status.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getByStatus(Friendship.FriendshipStatus status) {
        Account account = SecurityUtils.getCurrentAccount();
        return friendshipRepository.findByAccountAndStatus(account, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FriendshipResponse updateStatus(Long friendshipId, Friendship.FriendshipStatus status) {
        Account account = SecurityUtils.getCurrentAccount();
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));

        // Only addressee can accept, requester or addressee can block
        if (status == Friendship.FriendshipStatus.ACCEPTED && 
            !friendship.getAddressee().getId().equals(account.getId())) {
            throw new RuntimeException("Only addressee can accept friend request");
        }

        friendship.setStatus(status);
        friendship = friendshipRepository.save(friendship);
        
        return mapToResponse(friendship);
    }

    @Transactional
    public void delete(Long friendshipId) {
        Account account = SecurityUtils.getCurrentAccount();
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));

        if (!friendship.getRequester().getId().equals(account.getId()) &&
            !friendship.getAddressee().getId().equals(account.getId())) {
            throw new RuntimeException("Not authorized to delete this friendship");
        }

        friendshipRepository.delete(friendship);
    }

    private FriendshipResponse mapToResponse(Friendship friendship) {
        return FriendshipResponse.builder()
                .friendshipId(friendship.getId())
                .requesterId(friendship.getRequester().getId())
                .requesterEmail(friendship.getRequester().getEmail())
                .addresseeId(friendship.getAddressee().getId())
                .addresseeEmail(friendship.getAddressee().getEmail())
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .build();
    }
}
