package fingenie.com.fingenie.service;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.UserProfile;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.FriendshipRepository;
import fingenie.com.fingenie.repository.UserProfileRepository;
import fingenie.com.fingenie.dto.UserProfileRequest;
import fingenie.com.fingenie.dto.UserProfileResponse;
import fingenie.com.fingenie.dto.UserSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional
    public UserProfileResponse createOrUpdate(UserProfileRequest request) {
        Account account = SecurityUtils.getCurrentAccount();
        
        UserProfile profile = userProfileRepository.findByAccount(account)
                .orElse(UserProfile.builder()
                        .account(account)
                        .build());
        
        profile.setFullName(request.getFullName());
        profile.setDateOfBirth(request.getDateOfBirth());
        
        profile = userProfileRepository.save(profile);
        
        return mapToResponse(profile);
    }

    /**
     * Get current user's profile.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        Account account = SecurityUtils.getCurrentAccount();
        UserProfile profile = userProfileRepository.findByAccount(account)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User profile not found"));
        
        return mapToResponse(profile);
    }

    /**
     * Get profile by account ID.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getByAccountId(Long accountId) {
        UserProfile profile = userProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User profile not found"));
        
        return mapToResponse(profile);
    }

    /**
     * Search users by email or name for friend discovery
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String query, int page, int size) {
        Account currentAccount = SecurityUtils.getCurrentAccount();
        Long currentUserId = currentAccount.getId();
        
        Page<Account> results = accountRepository.searchByEmailOrName(
            query, currentUserId, PageRequest.of(page, size)
        );
        
        return results.getContent().stream()
            .map(account -> UserSearchResponse.builder()
                .userId(account.getId())
                .email(account.getEmail())
                .name(account.getName())
                .avatarUrl(account.getAvatarUrl())
                .isFriend(friendshipRepository.areFriends(currentUserId, account.getId()))
                .hasPendingRequest(friendshipRepository.hasPendingRequest(currentUserId, account.getId()))
                .build())
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete() {
        Account account = SecurityUtils.getCurrentAccount();
        userProfileRepository.findByAccount(account)
                .ifPresent(userProfileRepository::delete);
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .userId(profile.getId())
                .accountId(profile.getAccount().getId())
                .fullName(profile.getFullName())
                .dateOfBirth(profile.getDateOfBirth())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
