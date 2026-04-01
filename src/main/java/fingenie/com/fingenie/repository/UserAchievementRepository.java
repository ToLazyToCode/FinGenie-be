package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.UserAchievement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByAccountId(Long accountId);

    Optional<UserAchievement> findByAccountIdAndAchievementId(Long accountId, Long achievementId);

    List<UserAchievement> findByAccountIdAndIsUnlockedTrue(Long accountId);

    List<UserAchievement> findByAccountIdAndIsUnlockedTrueAndIsClaimedFalse(Long accountId);

    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.accountId = :accountId AND ua.isUnlocked = true")
    long countUnlockedByAccountId(@Param("accountId") Long accountId);
    
    long countByAccountId(Long accountId);

    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.accountId = :accountId AND ua.isUnlocked = true AND ua.isClaimed = false")
    long countClaimableByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT SUM(ua.progressValue) FROM UserAchievement ua WHERE ua.accountId = :accountId")
    Long sumProgressByAccountId(@Param("accountId") Long accountId);
    
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.accountId = :accountId AND ua.isUnlocked = true ORDER BY ua.createdAt DESC")
    List<UserAchievement> findRecentByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
