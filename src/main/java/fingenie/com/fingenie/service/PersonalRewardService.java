package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.dto.PersonalRewardCatalogItemResponse;
import fingenie.com.fingenie.dto.PersonalRewardOwnedResponse;
import fingenie.com.fingenie.dto.PersonalRewardRedeemResponse;
import fingenie.com.fingenie.entity.FinPointLedgerEntry;
import fingenie.com.fingenie.entity.PersonalVoucherRedemption;
import fingenie.com.fingenie.entity.RewardCatalogItem;
import fingenie.com.fingenie.repository.FinPointLedgerEntryRepository;
import fingenie.com.fingenie.repository.PersonalVoucherRedemptionRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalRewardService {

    private static final int DEFAULT_OWNED_REWARDS_LIMIT = 20;
    private static final int MAX_OWNED_REWARDS_LIMIT = 100;

    private final RewardCatalogService rewardCatalogService;
    private final PersonalVoucherRedemptionRepository personalVoucherRedemptionRepository;
    private final FinPointLedgerEntryRepository finPointLedgerEntryRepository;

    @Transactional
    public List<PersonalRewardCatalogItemResponse> getPersonalCatalog(Long accountId) {
        rewardCatalogService.ensureSeedData();

        List<RewardCatalogItem> catalog = rewardCatalogService.listActivePersonalCatalog();
        List<Long> catalogRewardIds = catalog.stream()
                .map(RewardCatalogItem::getId)
                .collect(Collectors.toList());
        Map<Long, PersonalVoucherRedemption> ownedByRewardId = catalogRewardIds.isEmpty()
                ? Map.of()
                : personalVoucherRedemptionRepository
                        .findByAccountIdAndRewardCatalogIdIn(accountId, catalogRewardIds)
                        .stream()
                        .collect(Collectors.toMap(
                                redemption -> redemption.getRewardCatalog().getId(),
                                Function.identity(),
                                (first, ignored) -> first
                        ));

        long currentBalance = safeLong(finPointLedgerEntryRepository.sumAmountByAccountId(accountId));
        return catalog.stream()
                .map(item -> toCatalogResponse(item, ownedByRewardId.get(item.getId()), currentBalance))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PersonalRewardOwnedResponse> getOwnedRewards(Long accountId, Integer limit) {
        rewardCatalogService.ensureSeedData();
        int normalizedLimit = normalizeLimit(limit, DEFAULT_OWNED_REWARDS_LIMIT, MAX_OWNED_REWARDS_LIMIT);

        return personalVoucherRedemptionRepository
                .findByAccountIdOrderByClaimedAtDesc(accountId, PageRequest.of(0, normalizedLimit))
                .stream()
                .map(this::toOwnedResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PersonalRewardRedeemResponse redeem(Long accountId, Long rewardId) {
        rewardCatalogService.ensureSeedData();

        RewardCatalogItem reward = rewardCatalogService.findActivePersonalById(rewardId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "rewardId", "notFoundOrNotPersonal",
                        "value", rewardId
                )));

        long cost = normalizeCost(reward.getPointCost());
        if (cost <= 0) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "rewardId", rewardId,
                    "reason", "invalidPointCost"
            ));
        }

        Optional<PersonalVoucherRedemption> existing = personalVoucherRedemptionRepository
                .findByAccountIdAndRewardCatalogId(accountId, rewardId);
        if (existing.isPresent()) {
            long balance = safeLong(finPointLedgerEntryRepository.sumAmountByAccountId(accountId));
            return toRedeemResponse(existing.get(), reward, false, balance);
        }

        String idempotencyKey = buildRedemptionIdempotencyKey(accountId, rewardId);
        FinPointLedgerEntry deductionEntry = findOrCreateDeductionEntry(accountId, reward, cost, idempotencyKey);

        PersonalVoucherRedemption redemption = PersonalVoucherRedemption.builder()
                .accountId(accountId)
                .rewardCatalog(reward)
                .status(fingenie.com.fingenie.entity.RewardStatus.CLAIMED)
                .finPointCost(cost)
                .finPointLedgerEntryId(deductionEntry.getId())
                .idempotencyKey(idempotencyKey)
                .claimedAt(Timestamp.from(Instant.now()))
                .expiresAt(reward.getExpiresAt())
                .build();

        boolean granted = true;
        try {
            redemption = personalVoucherRedemptionRepository.save(redemption);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Personal reward duplicate redemption blocked accountId={} rewardId={}", accountId, rewardId);
            redemption = personalVoucherRedemptionRepository.findByAccountIdAndRewardCatalogId(accountId, rewardId)
                    .orElseThrow(() -> ex);
            granted = false;
        }

        long balanceAfter = safeLong(finPointLedgerEntryRepository.sumAmountByAccountId(accountId));
        return toRedeemResponse(redemption, reward, granted, balanceAfter);
    }

    private FinPointLedgerEntry findOrCreateDeductionEntry(
            Long accountId,
            RewardCatalogItem reward,
            long cost,
            String idempotencyKey
    ) {
        Optional<FinPointLedgerEntry> existingEntry = finPointLedgerEntryRepository.findByIdempotencyKey(idempotencyKey);
        if (existingEntry.isPresent()) {
            return existingEntry.get();
        }

        long balance = safeLong(finPointLedgerEntryRepository.sumAmountByAccountId(accountId));
        if (balance < cost) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "reason", "insufficientFinPoint",
                    "currentBalance", balance,
                    "requiredCost", cost
            ));
        }

        FinPointLedgerEntry deduction = FinPointLedgerEntry.builder()
                .accountId(accountId)
                .amount(-cost)
                .sourceType(FinPointLedgerEntry.SourceType.VOUCHER_REDEMPTION)
                .sourceRefType("PERSONAL_VOUCHER")
                .sourceRefId(reward.getCode())
                .reason("personal_voucher_redemption")
                .idempotencyKey(idempotencyKey)
                .build();

        try {
            return finPointLedgerEntryRepository.save(deduction);
        } catch (DataIntegrityViolationException ex) {
            return finPointLedgerEntryRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    private PersonalRewardCatalogItemResponse toCatalogResponse(
            RewardCatalogItem item,
            PersonalVoucherRedemption owned,
            long currentBalance
    ) {
        long cost = normalizeCost(item.getPointCost());
        boolean ownedFlag = owned != null;
        boolean canRedeem = !ownedFlag && cost > 0 && currentBalance >= cost;
        return PersonalRewardCatalogItemResponse.builder()
                .rewardId(item.getId())
                .code(item.getCode())
                .title(item.getTitle())
                .description(item.getDescription())
                .category(item.getCategory())
                .pointCost(item.getPointCost())
                .goalThemeTags(item.getGoalThemeTags())
                .partnerName(item.getPartnerName())
                .partnerMetadata(item.getPartnerMetadata())
                .imageUrl(item.getImageUrl())
                .termsUrl(item.getTermsUrl())
                .expiresAt(item.getExpiresAt())
                .owned(ownedFlag)
                .ownedStatus(owned == null ? null : owned.getStatus())
                .canRedeem(canRedeem)
                .build();
    }

    private PersonalRewardOwnedResponse toOwnedResponse(PersonalVoucherRedemption redemption) {
        RewardCatalogItem item = redemption.getRewardCatalog();
        return PersonalRewardOwnedResponse.builder()
                .redemptionId(redemption.getId())
                .rewardId(item.getId())
                .code(item.getCode())
                .title(item.getTitle())
                .description(item.getDescription())
                .category(item.getCategory())
                .status(redemption.getStatus())
                .finPointCost(redemption.getFinPointCost())
                .claimedAt(redemption.getClaimedAt())
                .redeemedAt(redemption.getRedeemedAt())
                .expiresAt(redemption.getExpiresAt())
                .build();
    }

    private PersonalRewardRedeemResponse toRedeemResponse(
            PersonalVoucherRedemption redemption,
            RewardCatalogItem reward,
            boolean granted,
            long balanceAfter
    ) {
        return PersonalRewardRedeemResponse.builder()
                .rewardId(reward.getId())
                .code(reward.getCode())
                .title(reward.getTitle())
                .category(reward.getCategory())
                .granted(granted)
                .status(redemption.getStatus())
                .finPointCost(redemption.getFinPointCost())
                .balanceAfter(balanceAfter)
                .redemptionId(redemption.getId())
                .claimedAt(redemption.getClaimedAt())
                .build();
    }

    private String buildRedemptionIdempotencyKey(Long accountId, Long rewardId) {
        return "finpoint:redeem:" + accountId + ":" + rewardId;
    }

    private long normalizeCost(Long value) {
        return value == null ? 0L : value;
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
