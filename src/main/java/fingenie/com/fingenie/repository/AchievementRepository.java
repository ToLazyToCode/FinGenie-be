package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Achievement;
import fingenie.com.fingenie.entity.Achievement.AchievementCategory;
import fingenie.com.fingenie.entity.Achievement.AchievementTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByCode(String code);

    List<Achievement> findByIsActiveTrue();

    List<Achievement> findByIsActiveTrueAndIsHiddenFalse();

    List<Achievement> findByCategory(AchievementCategory category);

    List<Achievement> findByTier(AchievementTier tier);

    List<Achievement> findByCategoryAndIsActiveTrue(AchievementCategory category);

    @Query("SELECT a FROM Achievement a WHERE a.isActive = true ORDER BY a.sortOrder, a.tier, a.name")
    List<Achievement> findAllActiveSorted();

    @Query("SELECT a FROM Achievement a WHERE a.id NOT IN " +
           "(SELECT ua.achievementId FROM UserAchievement ua WHERE ua.accountId = :accountId AND ua.isUnlocked = true) " +
           "AND a.isActive = true")
    List<Achievement> findUnlockedAchievementsForUser(@Param("accountId") Long accountId);
}
