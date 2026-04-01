package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    
    private LeaderboardType type;
    private String period;
    private List<LeaderboardEntry> entries;
    private UserRankInfo currentUserRank;
    private long totalParticipants;
    
    public enum LeaderboardType {
        GLOBAL_XP,
        GLOBAL_LEVEL,
        FRIENDS,
        WEEKLY_XP,
        MONTHLY_SAVINGS,
        STREAK_CHAMPIONS
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private int rank;
        private Long accountId;
        private String accountName;
        private String avatarUrl;
        private int level;
        private int xp;
        private int lifetimeXp;
        private int currentStreak;
        private String badge;
        private boolean isCurrentUser;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRankInfo {
        private int rank;
        private int level;
        private int xp;
        private int lifetimeXp;
        private int percentile;
        private int pointsToNextRank;
        private String nextRankPlayerName;
        private String badge;
        private int friendsCount;
        private int rankAmongFriends;
    }
}
