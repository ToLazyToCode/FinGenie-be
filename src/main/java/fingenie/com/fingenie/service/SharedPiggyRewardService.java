package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.AuthenticationExceptions;
import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.dto.SharedPiggyRewardResponse;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.RewardCatalogItem;
import fingenie.com.fingenie.entity.RewardStatus;
import fingenie.com.fingenie.entity.SharedPiggyRewardUnlock;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import fingenie.com.fingenie.repository.SharedPiggyRewardUnlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SharedPiggyRewardService {

    public static final String GOALBOND_TARGET_MILESTONE_KEY = "GOALBOND_TARGET_REACHED_V1";
    private static final int DEFAULT_UNLOCKED_REWARDS_LIMIT = 20;
    private static final int MAX_UNLOCKED_REWARDS_LIMIT = 100;

    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;
    private final SharedPiggyRewardUnlockRepository sharedPiggyRewardUnlockRepository;
    private final RewardCatalogService rewardCatalogService;

    @Transactional(readOnly = true)
    public List<SharedPiggyRewardResponse> listUnlockedRewards(Long piggyId, Long accountId) {
        return listUnlockedRewards(piggyId, accountId, null);
    }

    @Transactional(readOnly = true)
    public List<SharedPiggyRewardResponse> listUnlockedRewards(Long piggyId, Long accountId, Integer limit) {
        resolveSharedPiggyForMember(piggyId, accountId);
        int normalizedLimit = normalizeLimit(limit, DEFAULT_UNLOCKED_REWARDS_LIMIT, MAX_UNLOCKED_REWARDS_LIMIT);

        return sharedPiggyRewardUnlockRepository
                .findByPiggyBankIdOrderByUnlockedAtDesc(piggyId, PageRequest.of(0, normalizedLimit))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<SharedPiggyRewardUnlock> unlockOnGoalBondThreshold(PiggyBank piggyBank, Long currentProgress, Long targetProgress) {
        if (piggyBank == null || !piggyBank.isShared()) {
            return Optional.empty();
        }

        long current = safeLong(currentProgress);
        long target = Math.max(1L, safeLong(targetProgress));
        if (current < target) {
            return Optional.empty();
        }

        rewardCatalogService.ensureSeedData();

        Optional<SharedPiggyRewardUnlock> existing = sharedPiggyRewardUnlockRepository
                .findByPiggyBankIdAndMilestoneKey(piggyBank.getId(), GOALBOND_TARGET_MILESTONE_KEY);
        if (existing.isPresent()) {
            return existing;
        }

        RewardCatalogItem selectedReward = rewardCatalogService.selectSharedRewardForPiggy(piggyBank);
        SharedPiggyRewardUnlock unlock = SharedPiggyRewardUnlock.builder()
                .piggyBank(piggyBank)
                .rewardCatalog(selectedReward)
                .milestoneKey(GOALBOND_TARGET_MILESTONE_KEY)
                .status(RewardStatus.AVAILABLE)
                .goalBondProgressAtUnlock(current)
                .goalBondTargetAtUnlock(target)
                .unlockedAt(Timestamp.from(Instant.now()))
                .expiresAt(selectedReward.getExpiresAt())
                .build();
        try {
            unlock = sharedPiggyRewardUnlockRepository.save(unlock);
            log.info(
                    "Shared piggy reward unlocked piggyId={} rewardCode={} milestone={}",
                    piggyBank.getId(),
                    selectedReward.getCode(),
                    GOALBOND_TARGET_MILESTONE_KEY
            );
            return Optional.of(unlock);
        } catch (DataIntegrityViolationException ex) {
            return sharedPiggyRewardUnlockRepository
                    .findByPiggyBankIdAndMilestoneKey(piggyBank.getId(), GOALBOND_TARGET_MILESTONE_KEY);
        }
    }

    private PiggyBank resolveSharedPiggyForMember(Long piggyId, Long accountId) {
        PiggyBank piggyBank = piggyBankRepository.findById(piggyId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "piggyId", "notFound",
                        "value", piggyId
                )));

        if (!piggyBank.isShared()) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "piggyId", piggyId,
                    "reason", "notShared"
            ));
        }

        boolean isOwner = piggyBank.getWallet() != null
                && piggyBank.getWallet().getAccount() != null
                && piggyBank.getWallet().getAccount().getId().equals(accountId);
        boolean isMember = piggyBankMemberRepository.existsByPiggyBankIdAndAccountId(piggyId, accountId);
        if (!isOwner && !isMember) {
            throw new AuthenticationExceptions.AccessDeniedException("shared-piggy-rewards");
        }
        return piggyBank;
    }

    private SharedPiggyRewardResponse toResponse(SharedPiggyRewardUnlock unlock) {
        RewardCatalogItem reward = unlock.getRewardCatalog();
        return SharedPiggyRewardResponse.builder()
                .unlockId(unlock.getId())
                .piggyId(unlock.getPiggyBank().getId())
                .milestoneKey(unlock.getMilestoneKey())
                .status(unlock.getStatus())
                .goalBondProgressAtUnlock(unlock.getGoalBondProgressAtUnlock())
                .goalBondTargetAtUnlock(unlock.getGoalBondTargetAtUnlock())
                .unlockedAt(unlock.getUnlockedAt())
                .expiresAt(unlock.getExpiresAt())
                .rewardId(reward.getId())
                .code(reward.getCode())
                .title(reward.getTitle())
                .description(reward.getDescription())
                .category(reward.getCategory())
                .goalThemeTags(reward.getGoalThemeTags())
                .partnerName(reward.getPartnerName())
                .partnerMetadata(reward.getPartnerMetadata())
                .imageUrl(reward.getImageUrl())
                .termsUrl(reward.getTermsUrl())
                .build();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int normalizeLimit(Integer requestedLimit, int defaultLimit, int maxLimit) {
        if (requestedLimit == null) {
            return defaultLimit;
        }
        return Math.min(Math.max(requestedLimit, 1), maxLimit);
    }
}
