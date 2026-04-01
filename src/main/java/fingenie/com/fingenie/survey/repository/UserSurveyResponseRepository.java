package fingenie.com.fingenie.survey.repository;

import fingenie.com.fingenie.survey.entity.UserSurveyResponse;
import fingenie.com.fingenie.survey.enums.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSurveyResponseRepository extends JpaRepository<UserSurveyResponse, Long> {
    
    /**
     * Find user's latest survey response.
     */
    Optional<UserSurveyResponse> findByUserIdAndIsLatestTrue(Long userId);
    
    /**
     * Find user's latest completed survey response.
     */
    Optional<UserSurveyResponse> findByUserIdAndIsLatestTrueAndStatus(Long userId, SurveyStatus status);
    
    /**
     * Find in-progress survey for a user.
     */
    @Query("SELECT usr FROM UserSurveyResponse usr WHERE usr.userId = :userId " +
           "AND usr.status = 'IN_PROGRESS' AND usr.isLatest = true")
    Optional<UserSurveyResponse> findInProgressSurvey(@Param("userId") Long userId);
    
    /**
     * Find all survey responses for a user (for audit/history).
     */
    List<UserSurveyResponse> findByUserIdOrderByResponseVersionDesc(Long userId);
    
    /**
     * Get current response version for a user.
     */
    @Query("SELECT COALESCE(MAX(usr.responseVersion), 0) FROM UserSurveyResponse usr WHERE usr.userId = :userId")
    int getCurrentVersion(@Param("userId") Long userId);
    
    /**
     * Mark all existing responses as not latest (before creating new one).
     */
    @Modifying
    @Query("UPDATE UserSurveyResponse usr SET usr.isLatest = false, usr.status = 'SUPERSEDED' " +
           "WHERE usr.userId = :userId AND usr.isLatest = true")
    void markExistingAsSuperseded(@Param("userId") Long userId);
    
    /**
     * Delete all responses for a user (GDPR deletion).
     */
    @Modifying
    @Query("DELETE FROM UserSurveyResponse usr WHERE usr.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    /**
     * Expire stale in-progress surveys.
     */
    @Modifying
    @Query("UPDATE UserSurveyResponse usr SET usr.status = 'EXPIRED' " +
           "WHERE usr.status = 'IN_PROGRESS' AND usr.expiresAt < :now")
    int expireStaleSurveys(@Param("now") LocalDateTime now);
    
    /**
     * Check if user has completed a survey.
     */
    boolean existsByUserIdAndStatus(Long userId, SurveyStatus status);
}
