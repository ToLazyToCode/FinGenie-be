package fingenie.com.fingenie.survey.repository;

import fingenie.com.fingenie.survey.entity.UserBehaviorProfile;
import fingenie.com.fingenie.survey.enums.BehavioralSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserBehaviorProfileRepository extends JpaRepository<UserBehaviorProfile, Long> {
    
    Optional<UserBehaviorProfile> findByUserId(Long userId);
    
    List<UserBehaviorProfile> findBySegment(BehavioralSegment segment);
    
    /**
     * Find high-risk users (for proactive interventions).
     */
    @Query("SELECT ubp FROM UserBehaviorProfile ubp WHERE ubp.overspendingScore >= :threshold " +
           "OR ubp.debtRiskScore >= :threshold ORDER BY ubp.overspendingScore DESC")
    List<UserBehaviorProfile> findHighRiskUsers(@Param("threshold") BigDecimal threshold);
    
    /**
     * Find profiles not synced to AI service.
     */
    @Query("SELECT ubp FROM UserBehaviorProfile ubp WHERE ubp.syncedToAiAt IS NULL " +
           "OR ubp.syncedToAiAt < ubp.updatedAt")
    List<UserBehaviorProfile> findPendingAiSync();
    
    /**
     * Find profiles needing refresh (older than specified days).
     */
    @Query("SELECT ubp FROM UserBehaviorProfile ubp WHERE ubp.surveyCompletedAt < :cutoffDate")
    List<UserBehaviorProfile> findProfilesNeedingRefresh(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Update AI sync timestamp.
     */
    @Modifying
    @Query("UPDATE UserBehaviorProfile ubp SET ubp.syncedToAiAt = :syncTime WHERE ubp.userId = :userId")
    void markSyncedToAi(@Param("userId") Long userId, @Param("syncTime") LocalDateTime syncTime);
    
    /**
     * Delete profile for a user (GDPR deletion).
     */
    @Modifying
    @Query("DELETE FROM UserBehaviorProfile ubp WHERE ubp.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * Get segment distribution (for analytics).
     */
    @Query("SELECT ubp.segment, COUNT(ubp) FROM UserBehaviorProfile ubp GROUP BY ubp.segment")
    List<Object[]> getSegmentDistribution();
    
    /**
     * Get average scores by segment (for benchmarking).
     */
    @Query("SELECT ubp.segment, AVG(ubp.overspendingScore), AVG(ubp.debtRiskScore), " +
           "AVG(ubp.savingsCapacityScore), AVG(ubp.financialAnxietyIndex) " +
           "FROM UserBehaviorProfile ubp GROUP BY ubp.segment")
    List<Object[]> getAverageScoresBySegment();
}
