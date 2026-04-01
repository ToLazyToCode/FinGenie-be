package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.UserGamification;
import fingenie.com.fingenie.dto.LeaderboardResponse;
import fingenie.com.fingenie.dto.LeaderboardResponse.LeaderboardEntry;
import fingenie.com.fingenie.dto.LeaderboardResponse.LeaderboardType;
import fingenie.com.fingenie.dto.LeaderboardResponse.UserRankInfo;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.FriendshipRepository;
import fingenie.com.fingenie.repository.UserGamificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
@Slf4j
public class LeaderboardService {

    private final UserGamificationRepository userGamificationRepository;
    private final FriendshipRepository friendshipRepository;
    private final AccountRepository accountRepository;
    private static final int TOP_LEADERBOARD_SIZE = 10;

    public LeaderboardService(UserGamificationRepository userGamificationRepository,
                              FriendshipRepository friendshipRepository,
                              AccountRepository accountRepository) {
        this.userGamificationRepository = userGamificationRepository;
        this.friendshipRepository = friendshipRepository;
        this.accountRepository = accountRepository;
    }

    public LeaderboardResponse getGlobalLeaderboard(Long currentAccountId, int limit) {
        List<UserGamification> allProfiles = getProfilesSortedByXp();
        List<UserGamification> topUsers = allProfiles.stream()
                .limit(Math.max(1, limit))
                .toList();

        Map<Long, Account> accountMap = loadAccountMap(topUsers);
        int myRank = getXpRank(allProfiles, currentAccountId);
        UserGamification myProfile = getProfileOrDefault(currentAccountId);

        List<LeaderboardEntry> entries = IntStream.range(0, topUsers.size())
                .mapToObj(i -> {
                    UserGamification profile = topUsers.get(i);
                    Account account = accountMap.get(profile.getAccountId());
                    return LeaderboardEntry.builder()
                            .rank(i + 1)
                            .accountId(profile.getAccountId())
                            .accountName(account != null ? account.getName() : "User " + profile.getAccountId())
                            .avatarUrl(account != null ? account.getAvatarUrl() : null)
                            .level(profile.getLevel())
                            .xp(profile.getXp())
                            .lifetimeXp(profile.getXp())
                            .badge(getLevelBadge(profile.getLevel()))
                            .isCurrentUser(profile.getAccountId().equals(currentAccountId))
                            .build();
                })
                .collect(Collectors.toList());

        UserRankInfo rankInfo = buildUserRankInfo(myProfile, myRank, allProfiles);
        log.debug("Built global leaderboard for account {} with {} participants", currentAccountId, allProfiles.size());

        return LeaderboardResponse.builder()
                .type(LeaderboardType.GLOBAL_XP)
                .period("All Time")
                .entries(entries)
                .currentUserRank(rankInfo)
                .totalParticipants(allProfiles.size())
                .build();
    }

    public LeaderboardResponse getFriendsLeaderboard(Long currentAccountId) {
        List<Long> friendIds = friendshipRepository.findFriendIds(currentAccountId);
        if (!friendIds.contains(currentAccountId)) {
            friendIds.add(currentAccountId);
        }

        List<UserGamification> friendProfiles = getProfilesSortedByXp().stream()
                .filter(profile -> friendIds.contains(profile.getAccountId()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (friendProfiles.stream().noneMatch(profile -> profile.getAccountId().equals(currentAccountId))) {
            friendProfiles.add(getProfileOrDefault(currentAccountId));
            friendProfiles = friendProfiles.stream()
                    .sorted(Comparator
                            .comparing(UserGamification::getXp, Comparator.nullsLast(Integer::compareTo))
                            .reversed()
                            .thenComparing(UserGamification::getLevel, Comparator.nullsLast(Integer::compareTo))
                            .reversed())
                    .toList();
        }

        final List<UserGamification> sortedFriendProfiles = friendProfiles;

        Map<Long, Account> accountMap = loadAccountMap(sortedFriendProfiles);

        List<LeaderboardEntry> entries = IntStream.range(0, sortedFriendProfiles.size())
                .mapToObj(i -> {
                    UserGamification profile = sortedFriendProfiles.get(i);
                    Account account = accountMap.get(profile.getAccountId());
                    return LeaderboardEntry.builder()
                            .rank(i + 1)
                            .accountId(profile.getAccountId())
                            .accountName(account != null ? account.getName() : "Friend")
                            .avatarUrl(account != null ? account.getAvatarUrl() : null)
                            .level(profile.getLevel())
                            .xp(profile.getXp())
                            .lifetimeXp(profile.getXp())
                            .badge(getLevelBadge(profile.getLevel()))
                            .isCurrentUser(profile.getAccountId().equals(currentAccountId))
                            .build();
                })
                .collect(Collectors.toList());

        int myRankAmongFriends = entries.stream()
                .filter(e -> e.getAccountId().equals(currentAccountId))
                .findFirst()
                .map(LeaderboardEntry::getRank)
                .orElse(0);

        UserGamification myProfile = getProfileOrDefault(currentAccountId);
        UserRankInfo rankInfo = UserRankInfo.builder()
                .rank(myRankAmongFriends)
                .level(myProfile.getLevel())
                .xp(myProfile.getXp())
                .lifetimeXp(myProfile.getXp())
                .percentile(calculatePercentile(myRankAmongFriends, sortedFriendProfiles.size()))
                .build();

        log.debug("Built friends leaderboard for account {} with {} participants", currentAccountId, sortedFriendProfiles.size());
        return LeaderboardResponse.builder()
                .type(LeaderboardType.FRIENDS)
                .period("Friends Ranking")
                .entries(entries)
                .currentUserRank(rankInfo)
                .totalParticipants(sortedFriendProfiles.size())
                .build();
    }

    public LeaderboardResponse getTopTen(Long currentAccountId) {
        return getGlobalLeaderboard(currentAccountId, TOP_LEADERBOARD_SIZE);
    }

    public LeaderboardResponse getLevelLeaderboard(Long currentAccountId, int limit) {
        List<UserGamification> allProfiles = getProfilesSortedByLevel();
        List<UserGamification> topUsers = allProfiles.stream()
                .limit(Math.max(1, limit))
                .toList();

        Map<Long, Account> accountMap = loadAccountMap(topUsers);
        int myRank = getLevelRank(allProfiles, currentAccountId);
        UserGamification myProfile = getProfileOrDefault(currentAccountId);

        List<LeaderboardEntry> entries = IntStream.range(0, topUsers.size())
                .mapToObj(i -> {
                    UserGamification profile = topUsers.get(i);
                    Account account = accountMap.get(profile.getAccountId());
                    return LeaderboardEntry.builder()
                            .rank(i + 1)
                            .accountId(profile.getAccountId())
                            .accountName(account != null ? account.getName() : "User " + profile.getAccountId())
                            .avatarUrl(account != null ? account.getAvatarUrl() : null)
                            .level(profile.getLevel())
                            .xp(profile.getXp())
                            .lifetimeXp(profile.getXp())
                            .badge(getLevelBadge(profile.getLevel()))
                            .isCurrentUser(profile.getAccountId().equals(currentAccountId))
                            .build();
                })
                .collect(Collectors.toList());

        UserRankInfo rankInfo = buildUserRankInfo(myProfile, myRank, allProfiles);
        return LeaderboardResponse.builder()
                .type(LeaderboardType.GLOBAL_LEVEL)
                .period("By Level")
                .entries(entries)
                .currentUserRank(rankInfo)
                .totalParticipants(allProfiles.size())
                .build();
    }

    private Map<Long, Account> loadAccountMap(List<UserGamification> profiles) {
        List<Long> accountIds = profiles.stream()
                .map(UserGamification::getAccountId)
                .collect(Collectors.toList());

        return accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
    }

    private UserRankInfo buildUserRankInfo(UserGamification myProfile, int myRank, List<UserGamification> orderedProfiles) {
        int pointsToNextRank = 0;
        String nextRankPlayerName = null;

        if (myRank > 1 && myRank - 2 < orderedProfiles.size()) {
            UserGamification nextUp = orderedProfiles.get(myRank - 2);
            pointsToNextRank = nextUp.getXp() - myProfile.getXp();
        }

        int totalUsers = Math.max(orderedProfiles.size(), 1);
        int percentile = calculatePercentile(myRank, totalUsers);
        return UserRankInfo.builder()
                .rank(myRank)
                .level(myProfile.getLevel())
                .xp(myProfile.getXp())
                .lifetimeXp(myProfile.getXp())
                .percentile(percentile)
                .pointsToNextRank(Math.max(0, pointsToNextRank))
                .nextRankPlayerName(nextRankPlayerName)
                .build();
    }

    private int calculatePercentile(int rank, int total) {
        if (total == 0 || rank == 0) return 0;
        return Math.max(0, Math.min(100, (int) Math.round((1.0 - ((double) rank / total)) * 100)));
    }

    private String getLevelBadge(int level) {
        if (level >= 50) return "LEGEND";
        if (level >= 40) return "MASTER";
        if (level >= 30) return "EXPERT";
        if (level >= 20) return "ADVANCED";
        if (level >= 10) return "INTERMEDIATE";
        if (level >= 5) return "BEGINNER";
        return "ROOKIE";
    }

    /**
     * Get the current user's rank information without retrieving the full leaderboard
     */
    public UserRankInfo getMyRank(Long currentAccountId) {
        List<UserGamification> orderedProfiles = getProfilesSortedByXp();
        UserGamification myProfile = getProfileOrDefault(currentAccountId);
        int myRank = getXpRank(orderedProfiles, currentAccountId);

        // Get nearby competitors for context
        int pointsToNextRank = 0;
        String nextRankPlayerName = null;

        if (myRank > 1 && myRank - 2 < orderedProfiles.size()) {
            UserGamification nextUp = orderedProfiles.get(myRank - 2);
            pointsToNextRank = nextUp.getXp() - myProfile.getXp();

            Account nextUpAccount = accountRepository.findById(nextUp.getAccountId()).orElse(null);
                nextRankPlayerName = nextUpAccount != null ? nextUpAccount.getName() : "Player #" + (myRank - 1);
        }

        int totalUsers = Math.max(orderedProfiles.size(), 1);
        int percentile = calculatePercentile(myRank, totalUsers);

        // Get friends count
        long friendsCount = friendshipRepository.countFriends(currentAccountId);

        // Get friends rank
        List<Long> friendIds = friendshipRepository.findFriendIds(currentAccountId);
        if (!friendIds.contains(currentAccountId)) {
            friendIds.add(currentAccountId);
        }
        List<UserGamification> friendLevels = getProfilesSortedByXp().stream()
                .filter(profile -> friendIds.contains(profile.getAccountId()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (friendLevels.stream().noneMatch(profile -> profile.getAccountId().equals(currentAccountId))) {
            friendLevels.add(myProfile);
            friendLevels = friendLevels.stream()
                    .sorted(Comparator
                            .comparing(UserGamification::getXp, Comparator.nullsLast(Integer::compareTo))
                            .reversed()
                            .thenComparing(UserGamification::getLevel, Comparator.nullsLast(Integer::compareTo))
                            .reversed())
                    .toList();
        }
        int rankAmongFriends = 1;
        for (int i = 0; i < friendLevels.size(); i++) {
            if (friendLevels.get(i).getAccountId().equals(currentAccountId)) {
                rankAmongFriends = i + 1;
                break;
            }
        }
        
        return UserRankInfo.builder()
                .rank(myRank)
                .level(myProfile.getLevel())
                .xp(myProfile.getXp())
                .lifetimeXp(myProfile.getXp())
                .percentile(percentile)
                .pointsToNextRank(Math.max(0, pointsToNextRank))
                .nextRankPlayerName(nextRankPlayerName)
                .badge(getLevelBadge(myProfile.getLevel()))
                .friendsCount((int) friendsCount)
                .rankAmongFriends(rankAmongFriends)
                .build();
    }

    private List<UserGamification> getProfilesSortedByXp() {
        return userGamificationRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(UserGamification::getXp, Comparator.nullsLast(Integer::compareTo))
                        .reversed()
                        .thenComparing(UserGamification::getLevel, Comparator.nullsLast(Integer::compareTo))
                        .reversed())
                .toList();
    }

    private List<UserGamification> getProfilesSortedByLevel() {
        return userGamificationRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(UserGamification::getLevel, Comparator.nullsLast(Integer::compareTo))
                        .reversed()
                        .thenComparing(UserGamification::getXp, Comparator.nullsLast(Integer::compareTo))
                        .reversed())
                .toList();
    }

    private UserGamification getProfileOrDefault(Long accountId) {
        return userGamificationRepository.findByAccountId(accountId)
                .orElseGet(() -> UserGamification.builder()
                        .accountId(accountId)
                        .level(1)
                        .xp(0)
                        .build());
    }

    private int getXpRank(List<UserGamification> orderedProfiles, Long accountId) {
        for (int i = 0; i < orderedProfiles.size(); i++) {
            if (Objects.equals(orderedProfiles.get(i).getAccountId(), accountId)) {
                return i + 1;
            }
        }
        UserGamification fallback = getProfileOrDefault(accountId);
        long higherCount = orderedProfiles.stream()
                .filter(profile -> profile.getXp() != null && profile.getXp() > fallback.getXp())
                .count();
        return (int) higherCount + 1;
    }

    private int getLevelRank(List<UserGamification> orderedProfiles, Long accountId) {
        for (int i = 0; i < orderedProfiles.size(); i++) {
            if (Objects.equals(orderedProfiles.get(i).getAccountId(), accountId)) {
                return i + 1;
            }
        }
        UserGamification fallback = getProfileOrDefault(accountId);
        long higherCount = orderedProfiles.stream()
                .filter(profile ->
                        profile.getLevel() != null && fallback.getLevel() != null && (
                                profile.getLevel() > fallback.getLevel() ||
                                (Objects.equals(profile.getLevel(), fallback.getLevel())
                                        && profile.getXp() != null
                                        && fallback.getXp() != null
                                        && profile.getXp() > fallback.getXp())
                        ))
                .count();
        return (int) higherCount + 1;
    }
}
