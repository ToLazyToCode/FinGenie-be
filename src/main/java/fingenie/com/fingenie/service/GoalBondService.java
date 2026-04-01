package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.AuthenticationExceptions;
import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.dto.GoalBondMissionClaimResponse;
import fingenie.com.fingenie.dto.GoalBondMissionStateResponse;
import fingenie.com.fingenie.dto.GoalBondSummaryResponse;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.entity.GoalBondMissionClaim;
import fingenie.com.fingenie.entity.GoalBondProgress;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.SavingContribution;
import fingenie.com.fingenie.repository.GoalBondMissionClaimRepository;
import fingenie.com.fingenie.repository.GoalBondProgressRepository;
import fingenie.com.fingenie.repository.PiggyBankMemberRepository;
import fingenie.com.fingenie.repository.PiggyBankRepository;
import fingenie.com.fingenie.repository.SavingContributionRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
public class GoalBondService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long DEFAULT_TARGET_PROGRESS = 100L;

    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankMemberRepository piggyBankMemberRepository;
    private final SavingContributionRepository savingContributionRepository;
    private final GoalBondProgressRepository goalBondProgressRepository;
    private final GoalBondMissionClaimRepository goalBondMissionClaimRepository;
    private final SharedPiggyRewardService sharedPiggyRewardService;
    private final EntitlementService entitlementService;

    @Transactional(readOnly = true)
    public GoalBondSummaryResponse getSummary(Long piggyId, Long accountId) {
        PiggyBank piggyBank = resolveSharedPiggyForMember(piggyId, accountId);
        GoalBondProgress progress = getOrCreateProgress(piggyBank);
        return toSummary(progress);
    }

    @Transactional(readOnly = true)
    public GoalBondMissionStateResponse getTodayMissionState(Long piggyId, Long accountId) {
        PiggyBank piggyBank = resolveSharedPiggyForMember(piggyId, accountId);
        LocalDate today = LocalDate.now(VN_ZONE);
        DayWindow window = buildDayWindow(today);

        Map<String, GoalBondMissionClaim> claimedByMission = goalBondMissionClaimRepository
                .findByPiggyBankIdAndAccountIdAndMissionDayOrderByCreatedAtDesc(piggyBank.getId(), accountId, today)
                .stream()
                .collect(Collectors.toMap(
                        GoalBondMissionClaim::getMissionId,
                        claim -> claim,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));

        List<GoalBondMissionStateResponse.MissionState> missions = Arrays.stream(SharedMission.values())
                .map(mission -> toMissionState(piggyBank.getId(), accountId, mission, window, claimedByMission))
                .collect(Collectors.toList());

        return GoalBondMissionStateResponse.builder()
                .piggyId(piggyBank.getId())
                .dayKey(today.toString())
                .missions(missions)
                .build();
    }

    @Transactional
    public GoalBondMissionClaimResponse claimMission(Long piggyId, Long accountId, String missionIdRaw) {
        SharedMission mission = SharedMission.fromMissionId(missionIdRaw)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "missionId", "unsupported",
                        "value", missionIdRaw
                )));

        PiggyBank piggyBank = resolveSharedPiggyForMember(piggyId, accountId);
        LocalDate today = LocalDate.now(VN_ZONE);
        DayWindow window = buildDayWindow(today);

        Optional<GoalBondMissionClaim> existing = goalBondMissionClaimRepository
                .findByPiggyBankIdAndAccountIdAndMissionIdAndMissionDay(
                        piggyBank.getId(),
                        accountId,
                        mission.getMissionId(),
                        today
                );
        if (existing.isPresent()) {
            GoalBondProgress progress = getOrCreateProgress(piggyBank);
            return toClaimResponse(piggyBank.getId(), today, mission, existing.get(), false, progress);
        }

        int progressCount = calculateProgressCount(piggyBank.getId(), accountId, mission, window);
        if (progressCount < mission.getRequiredCount()) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "missionId", mission.getMissionId(),
                    "reason", "notClaimable",
                    "requiredCount", mission.getRequiredCount(),
                    "progressCount", progressCount
            ));
        }

        GoalBondProgress goalBondProgress = getOrCreateProgressForUpdate(piggyBank);
        long scaledReward = scaleReward(
                mission.getRewardGoalBond(),
                entitlementService.getGroupMissionPointMultiplier(accountId)
        );
        GoalBondMissionClaim claim = GoalBondMissionClaim.builder()
                .piggyBank(piggyBank)
                .accountId(accountId)
                .missionId(mission.getMissionId())
                .missionDay(today)
                .rewardAmount(scaledReward)
                .progressSnapshot(progressCount)
                .idempotencyKey(buildIdempotencyKey(piggyBank.getId(), accountId, mission, today))
                .build();

        try {
            GoalBondMissionClaim saved = goalBondMissionClaimRepository.save(claim);
            boolean thresholdJustReached = applyProgressReward(goalBondProgress, scaledReward);
            goalBondProgressRepository.save(goalBondProgress);
            if (thresholdJustReached || goalBondProgress.getStatus() == GoalBondProgress.Status.TARGET_REACHED) {
                sharedPiggyRewardService.unlockOnGoalBondThreshold(
                        piggyBank,
                        goalBondProgress.getCurrentProgress(),
                        goalBondProgress.getTargetProgress()
                );
            }
            return toClaimResponse(piggyBank.getId(), today, mission, saved, true, goalBondProgress);
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "GoalBond duplicate claim blocked piggyId={} accountId={} missionId={} day={}",
                    piggyBank.getId(),
                    accountId,
                    mission.getMissionId(),
                    today
            );
            GoalBondMissionClaim alreadySaved = goalBondMissionClaimRepository
                    .findByPiggyBankIdAndAccountIdAndMissionIdAndMissionDay(
                            piggyBank.getId(),
                            accountId,
                            mission.getMissionId(),
                            today
                    )
                    .orElseThrow(() -> ex);
            GoalBondProgress refreshed = getOrCreateProgress(piggyBank);
            return toClaimResponse(piggyBank.getId(), today, mission, alreadySaved, false, refreshed);
        }
    }

    private GoalBondMissionStateResponse.MissionState toMissionState(
            Long piggyId,
            Long accountId,
            SharedMission mission,
            DayWindow dayWindow,
            Map<String, GoalBondMissionClaim> claimedByMission
    ) {
        GoalBondMissionClaim claimed = claimedByMission.get(mission.getMissionId());
        int progressCount = calculateProgressCount(piggyId, accountId, mission, dayWindow);
        boolean completed = claimed != null;

        return GoalBondMissionStateResponse.MissionState.builder()
                .missionId(mission.getMissionId())
                .requiredCount(mission.getRequiredCount())
                .progressCount(progressCount)
                .rewardGoalBond(scaleReward(
                        mission.getRewardGoalBond(),
                        entitlementService.getGroupMissionPointMultiplier(accountId)
                ))
                .claimable(!completed && progressCount >= mission.getRequiredCount())
                .completed(completed)
                .claimedAt(claimed == null ? null : claimed.getCreatedAt())
                .build();
    }

    private int calculateProgressCount(Long piggyId, Long accountId, SharedMission mission, DayWindow dayWindow) {
        if (mission.getEligibilityType() == MissionEligibilityType.SELF_CONTRIBUTION_COUNT) {
            return (int) savingContributionRepository.countByTargetTypeAndTargetIdAndAccountIdAndCreatedAtRange(
                    SavingContribution.TargetType.PIGGY,
                    piggyId,
                    accountId,
                    dayWindow.fromInclusive(),
                    dayWindow.toExclusive()
            );
        }

        return (int) savingContributionRepository.countDistinctAccountByTargetTypeAndTargetIdAndCreatedAtRange(
                SavingContribution.TargetType.PIGGY,
                piggyId,
                dayWindow.fromInclusive(),
                dayWindow.toExclusive()
        );
    }

    private boolean applyProgressReward(GoalBondProgress progress, long rewardAmount) {
        long current = progress.getCurrentProgress() == null ? 0L : progress.getCurrentProgress();
        long target = progress.getTargetProgress() == null ? DEFAULT_TARGET_PROGRESS : progress.getTargetProgress();
        boolean wasReached = current >= target
                || progress.getStatus() == GoalBondProgress.Status.TARGET_REACHED;
        long next = Math.min(target, current + rewardAmount);
        progress.setCurrentProgress(next);
        progress.setTargetProgress(target);
        boolean reachedNow = next >= target;
        progress.setStatus(reachedNow
                ? GoalBondProgress.Status.TARGET_REACHED
                : GoalBondProgress.Status.IN_PROGRESS);
        return !wasReached && reachedNow;
    }

    private GoalBondSummaryResponse toSummary(GoalBondProgress progress) {
        long current = progress.getCurrentProgress() == null ? 0L : progress.getCurrentProgress();
        long target = progress.getTargetProgress() == null ? DEFAULT_TARGET_PROGRESS : progress.getTargetProgress();
        int percent = target <= 0 ? 0 : (int) Math.min(100L, (current * 100L) / target);

        return GoalBondSummaryResponse.builder()
                .piggyId(progress.getPiggyBank().getId())
                .currentProgress(current)
                .targetProgress(target)
                .progressPercent(percent)
                .status(progress.getStatus().name())
                .build();
    }

    private GoalBondMissionClaimResponse toClaimResponse(
            Long piggyId,
            LocalDate missionDay,
            SharedMission mission,
            GoalBondMissionClaim claim,
            boolean granted,
            GoalBondProgress progress
    ) {
        long currentProgress = progress.getCurrentProgress() == null ? 0L : progress.getCurrentProgress();
        long targetProgress = progress.getTargetProgress() == null ? DEFAULT_TARGET_PROGRESS : progress.getTargetProgress();
        String status = progress.getStatus() == null
                ? GoalBondProgress.Status.IN_PROGRESS.name()
                : progress.getStatus().name();

        return GoalBondMissionClaimResponse.builder()
                .piggyId(piggyId)
                .missionId(mission.getMissionId())
                .dayKey(missionDay.toString())
                .granted(granted)
                .rewardAwarded(granted && claim.getRewardAmount() != null ? claim.getRewardAmount() : 0L)
                .currentProgress(currentProgress)
                .targetProgress(targetProgress)
                .status(status)
                .claimedAt(claim.getCreatedAt())
                .build();
    }

    private GoalBondProgress getOrCreateProgress(PiggyBank piggyBank) {
        return goalBondProgressRepository.findByPiggyBankId(piggyBank.getId())
                .orElseGet(() -> createDefaultProgress(piggyBank));
    }

    private GoalBondProgress getOrCreateProgressForUpdate(PiggyBank piggyBank) {
        Optional<GoalBondProgress> existing = goalBondProgressRepository.findByPiggyBankIdForUpdate(piggyBank.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        createDefaultProgress(piggyBank);
        return goalBondProgressRepository.findByPiggyBankIdForUpdate(piggyBank.getId())
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "piggyId", piggyBank.getId(),
                        "reason", "goalBondProgressMissing"
                )));
    }

    private GoalBondProgress createDefaultProgress(PiggyBank piggyBank) {
        GoalBondProgress draft = GoalBondProgress.builder()
                .piggyBank(piggyBank)
                .currentProgress(0L)
                .targetProgress(DEFAULT_TARGET_PROGRESS)
                .status(GoalBondProgress.Status.IN_PROGRESS)
                .build();
        try {
            return goalBondProgressRepository.save(draft);
        } catch (DataIntegrityViolationException ex) {
            return goalBondProgressRepository.findByPiggyBankId(piggyBank.getId())
                    .orElseThrow(() -> ex);
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
            throw new AuthenticationExceptions.AccessDeniedException("goalbond");
        }

        return piggyBank;
    }

    private DayWindow buildDayWindow(LocalDate missionDay) {
        Timestamp fromInclusive = Timestamp.from(missionDay.atStartOfDay(VN_ZONE).toInstant());
        Timestamp toExclusive = Timestamp.from(missionDay.plusDays(1).atStartOfDay(VN_ZONE).toInstant());
        return new DayWindow(fromInclusive, toExclusive);
    }

    private String buildIdempotencyKey(Long piggyId, Long accountId, SharedMission mission, LocalDate day) {
        return "goalbond:mission:" + piggyId + ":" + accountId + ":" + mission.getMissionId() + ":" + day;
    }

    private long scaleReward(long baseReward, double multiplier) {
        long scaled = Math.round(baseReward * Math.max(multiplier, 0d));
        return Math.max(0L, scaled);
    }

    private record DayWindow(Timestamp fromInclusive, Timestamp toExclusive) {}

    private enum MissionEligibilityType {
        SELF_CONTRIBUTION_COUNT,
        TEAM_CONTRIBUTOR_COUNT
    }

    @Getter
    private enum SharedMission {
        SHARED_CONTRIBUTE_ONCE(
                "sharedContributeOnce",
                MissionEligibilityType.SELF_CONTRIBUTION_COUNT,
                1,
                10L
        ),
        SHARED_CONTRIBUTE_THREE(
                "sharedContributeThree",
                MissionEligibilityType.SELF_CONTRIBUTION_COUNT,
                3,
                15L
        ),
        SHARED_TEAM_TWO_CONTRIBUTORS(
                "sharedTeamTwoContributors",
                MissionEligibilityType.TEAM_CONTRIBUTOR_COUNT,
                2,
                20L
        );

        private final String missionId;
        private final MissionEligibilityType eligibilityType;
        private final int requiredCount;
        private final long rewardGoalBond;

        SharedMission(
                String missionId,
                MissionEligibilityType eligibilityType,
                int requiredCount,
                long rewardGoalBond
        ) {
            this.missionId = missionId;
            this.eligibilityType = eligibilityType;
            this.requiredCount = requiredCount;
            this.rewardGoalBond = rewardGoalBond;
        }

        private static final Map<String, SharedMission> LOOKUP = Arrays.stream(values())
                .collect(Collectors.toMap(
                        mission -> mission.missionId.toLowerCase(Locale.ROOT),
                        mission -> mission
                ));

        static Optional<SharedMission> fromMissionId(String missionIdRaw) {
            if (missionIdRaw == null || missionIdRaw.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(LOOKUP.get(missionIdRaw.trim().toLowerCase(Locale.ROOT)));
        }
    }
}
