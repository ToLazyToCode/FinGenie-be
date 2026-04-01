package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    /**
     * Find all consents for a user
     */
    List<UserConsent> findByUserId(Long userId);

    /**
     * Find specific consent type for a user
     */
    Optional<UserConsent> findByUserIdAndConsentType(Long userId, String consentType);

    /**
     * Check if user has granted a specific consent type
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM UserConsent c WHERE c.userId = :userId AND c.consentType = :consentType " +
           "AND c.granted = true AND c.withdrawnAt IS NULL")
    boolean hasActiveConsent(@Param("userId") Long userId, @Param("consentType") String consentType);

    /**
     * Find users who have granted AI training consent
     */
    @Query("SELECT c.userId FROM UserConsent c WHERE c.consentType = 'AI_TRAINING' " +
           "AND c.granted = true AND c.withdrawnAt IS NULL")
    List<Long> findUsersWithAITrainingConsent();

    /**
     * Find all users with a specific consent type granted
     */
    @Query("SELECT c FROM UserConsent c WHERE c.consentType = :consentType " +
           "AND c.granted = true AND c.withdrawnAt IS NULL")
    List<UserConsent> findActiveConsentsByType(@Param("consentType") String consentType);

    /**
     * Withdraw all consents for a user (GDPR right to be forgotten)
     */
    @Modifying
    @Query("UPDATE UserConsent c SET c.granted = false, c.withdrawnAt = CURRENT_TIMESTAMP " +
           "WHERE c.userId = :userId AND c.granted = true")
    int withdrawAllConsents(@Param("userId") Long userId);

    /**
     * Count consents by type
     */
    @Query("SELECT c.consentType, " +
           "SUM(CASE WHEN c.granted = true AND c.withdrawnAt IS NULL THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.granted = false OR c.withdrawnAt IS NOT NULL THEN 1 ELSE 0 END) " +
           "FROM UserConsent c GROUP BY c.consentType")
    List<Object[]> countConsentsByType();

    /**
     * Find recently withdrawn consents (for data cleanup jobs)
     */
    @Query("SELECT c FROM UserConsent c WHERE c.withdrawnAt IS NOT NULL " +
           "AND c.withdrawnAt > :since ORDER BY c.withdrawnAt DESC")
    List<UserConsent> findRecentlyWithdrawn(@Param("since") java.time.LocalDateTime since);
}
