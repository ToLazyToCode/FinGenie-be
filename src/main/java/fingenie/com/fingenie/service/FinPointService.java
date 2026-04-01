package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.dto.FinPointHistoryItemResponse;
import fingenie.com.fingenie.dto.FinPointHistoryPageResponse;
import fingenie.com.fingenie.dto.FinPointMissionClaimResponse;
import fingenie.com.fingenie.dto.FinPointMissionStateResponse;
import fingenie.com.fingenie.dto.FinPointSummaryResponse;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.entity.FinPointLedgerEntry;
import fingenie.com.fingenie.repository.FinPointLedgerEntryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinPointService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FinPointLedgerEntryRepository finPointLedgerEntryRepository;
    private final EntitlementService entitlementService;

    @Transactional(readOnly = true)
    public FinPointSummaryResponse getSummary(Long accountId) {
        LocalDate today = LocalDate.now(VN_ZONE);
        long balance = safeLong(finPointLedgerEntryRepository.sumAmountByAccountId(accountId));
        Timestamp fromInclusive = Timestamp.from(today.atStartOfDay(VN_ZONE).toInstant());
        Timestamp toExclusive = Timestamp.from(today.plusDays(1).atStartOfDay(VN_ZONE).toInstant());
        long todayEarned = safeLong(
                finPointLedgerEntryRepository.sumAmountByAccountIdAndCreatedAtRange(
                        accountId,
                        fromInclusive,
                        toExclusive
                )
        );

        return FinPointSummaryResponse.builder()
                .accountId(accountId)
                .balance(balance)
                .todayEarned(todayEarned)
                .lifetimeEarned(balance)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FinPointHistoryItemResponse> getRecentHistory(Long accountId, int limit) {
        return getHistoryPage(accountId, 0, limit).getItems();
    }

    @Transactional(readOnly = true)
    public FinPointHistoryPageResponse getHistoryPage(Long accountId, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Page<FinPointLedgerEntry> dataPage = finPointLedgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(
                accountId,
                PageRequest.of(normalizedPage, normalizedSize)
        );

        List<FinPointHistoryItemResponse> items = dataPage.getContent().stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());

        return FinPointHistoryPageResponse.builder()
                .items(items)
                .page(normalizedPage)
                .size(normalizedSize)
                .totalItems(dataPage.getTotalElements())
                .hasNext(dataPage.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public FinPointMissionStateResponse getTodayMissionState(Long accountId) {
        LocalDate today = LocalDate.now(VN_ZONE);
        double rewardMultiplier = entitlementService.getPersonalMissionPointMultiplier(accountId);
        List<FinPointLedgerEntry> todayEntries = finPointLedgerEntryRepository
                .findByAccountIdAndSourceTypeAndMissionDayOrderByCreatedAtDesc(
                        accountId,
                        FinPointLedgerEntry.SourceType.DAILY_MISSION,
                        today
                );

        Map<String, FinPointLedgerEntry> claimedByMission = new LinkedHashMap<>();
        for (FinPointLedgerEntry entry : todayEntries) {
            if (entry.getMissionId() != null && !claimedByMission.containsKey(entry.getMissionId())) {
                claimedByMission.put(entry.getMissionId(), entry);
            }
        }

        List<FinPointMissionStateResponse.MissionRewardState> missionStates = Arrays.stream(MissionReward.values())
                .map(mission -> {
                    FinPointLedgerEntry claimedEntry = claimedByMission.get(mission.getMissionId());
                    return FinPointMissionStateResponse.MissionRewardState.builder()
                            .missionId(mission.getMissionId())
                            .completed(claimedEntry != null)
                            .xpReward(mission.getXpReward())
                            .finPointReward(scaleReward(mission.getFinPointReward(), rewardMultiplier))
                            .claimedAt(claimedEntry == null ? null : claimedEntry.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        int xpToday = missionStates.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                .mapToInt(item -> item.getXpReward() == null ? 0 : item.getXpReward())
                .sum();

        long finPointToday = missionStates.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCompleted()))
                .mapToLong(item -> item.getFinPointReward() == null ? 0L : item.getFinPointReward())
                .sum();

        return FinPointMissionStateResponse.builder()
                .dayKey(today.toString())
                .xpToday(xpToday)
                .finPointToday(finPointToday)
                .missions(missionStates)
                .build();
    }

    @Transactional
    public FinPointMissionClaimResponse claimDailyMission(Long accountId, String missionIdRaw) {
        MissionReward mission = MissionReward.fromMissionId(missionIdRaw)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "missionId", "unsupported",
                        "value", missionIdRaw
                )));

        LocalDate today = LocalDate.now(VN_ZONE);
        Optional<FinPointLedgerEntry> existing = finPointLedgerEntryRepository
                .findByAccountIdAndMissionIdAndMissionDay(accountId, mission.getMissionId(), today);
        if (existing.isPresent()) {
            return buildClaimResponse(accountId, mission, today, existing.get(), false);
        }

        long awardedPoints = scaleReward(
                mission.getFinPointReward(),
                entitlementService.getPersonalMissionPointMultiplier(accountId)
        );
        String idempotencyKey = buildMissionIdempotencyKey(accountId, mission, today);
        FinPointLedgerEntry entry = FinPointLedgerEntry.builder()
                .accountId(accountId)
                .amount(awardedPoints)
                .sourceType(FinPointLedgerEntry.SourceType.DAILY_MISSION)
                .sourceRefType("DAILY_MISSION")
                .sourceRefId(mission.getMissionId())
                .reason("daily_mission_reward")
                .missionId(mission.getMissionId())
                .missionDay(today)
                .idempotencyKey(idempotencyKey)
                .build();

        try {
            FinPointLedgerEntry saved = finPointLedgerEntryRepository.save(entry);
            return buildClaimResponse(accountId, mission, today, saved, true);
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "FinPoint duplicate mission claim blocked accountId={} missionId={} day={}",
                    accountId,
                    mission.getMissionId(),
                    today
            );
            FinPointLedgerEntry alreadySaved = finPointLedgerEntryRepository
                    .findByAccountIdAndMissionIdAndMissionDay(accountId, mission.getMissionId(), today)
                    .orElseThrow(() -> ex);
            return buildClaimResponse(accountId, mission, today, alreadySaved, false);
        }
    }

    private FinPointMissionClaimResponse buildClaimResponse(
            Long accountId,
            MissionReward mission,
            LocalDate missionDay,
            FinPointLedgerEntry entry,
            boolean granted
    ) {
        long balance = safeLong(finPointLedgerEntryRepository.sumAmountByAccountId(accountId));
        return FinPointMissionClaimResponse.builder()
                .missionId(mission.getMissionId())
                .dayKey(missionDay.toString())
                .granted(granted)
                .xpReward(mission.getXpReward())
                .finPointAwarded(entry.getAmount() == null ? 0L : entry.getAmount())
                .balance(balance)
                .claimedAt(entry.getCreatedAt())
                .build();
    }

    private String buildMissionIdempotencyKey(Long accountId, MissionReward mission, LocalDate day) {
        return "finpoint:mission:" + accountId + ":" + mission.getMissionId() + ":" + day;
    }

    private FinPointHistoryItemResponse toHistoryItem(FinPointLedgerEntry entry) {
        return FinPointHistoryItemResponse.builder()
                .id(entry.getId())
                .amount(entry.getAmount())
                .sourceType(entry.getSourceType().name())
                .sourceRefType(entry.getSourceRefType())
                .sourceRefId(entry.getSourceRefId())
                .reason(entry.getReason())
                .missionId(entry.getMissionId())
                .missionDay(entry.getMissionDay() == null ? null : entry.getMissionDay().toString())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private long scaleReward(long baseReward, double multiplier) {
        long scaled = Math.round(baseReward * Math.max(multiplier, 0d));
        return Math.max(scaled, 0L);
    }

    @Getter
    private enum MissionReward {
        CONTRIBUTE_TODAY("contributeToday", 10, 5L),
        VIEW_PLAN("viewPlan", 10, 5L),
        VIEW_ACTIVITY("viewActivity", 10, 5L);

        private final String missionId;
        private final int xpReward;
        private final long finPointReward;

        MissionReward(String missionId, int xpReward, long finPointReward) {
            this.missionId = missionId;
            this.xpReward = xpReward;
            this.finPointReward = finPointReward;
        }

        private static final Map<String, MissionReward> LOOKUP = Arrays.stream(values())
                .collect(Collectors.toMap(
                        mission -> mission.missionId.toLowerCase(Locale.ROOT),
                        mission -> mission
                ));

        static Optional<MissionReward> fromMissionId(String missionIdRaw) {
            if (missionIdRaw == null || missionIdRaw.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(LOOKUP.get(missionIdRaw.trim().toLowerCase(Locale.ROOT)));
        }
    }
}
