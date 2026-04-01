package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.SpendingGuess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpendingGuessRepository extends JpaRepository<SpendingGuess, Long> {

    /**
     * Find the most recent guess for a user with given status
     */
    Optional<SpendingGuess> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    Optional<SpendingGuess> findFirstByCreatedTransactionId(Long createdTransactionId);

    /**
     * Find all pending guesses for a user
     */
    List<SpendingGuess> findByUserIdAndStatusOrderByGuessedForTimeAsc(Long userId, String status);

    /**
     * Find all guesses for a user today
     */
    @Query("SELECT g FROM SpendingGuess g WHERE g.userId = :userId " +
           "AND g.guessedForTime >= :startOfDay AND g.guessedForTime < :endOfDay")
    List<SpendingGuess> findTodayGuesses(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Find expired pending guesses
     */
    @Query("SELECT g FROM SpendingGuess g WHERE g.status = 'PENDING' AND g.expiresAt < :now")
    List<SpendingGuess> findExpiredGuesses(@Param("now") LocalDateTime now);

    /**
     * Expire all pending guesses past their expiration
     */
    @Modifying
    @Query("UPDATE SpendingGuess g SET g.status = 'EXPIRED' WHERE g.status = 'PENDING' AND g.expiresAt < :now")
    int expireOldGuesses(@Param("now") LocalDateTime now);

    /**
     * Count guesses by status for a user
     */
    long countByUserIdAndStatus(Long userId, String status);

    /**
     * Find guesses for feedback analysis (last N days)
     */
    @Query("SELECT g FROM SpendingGuess g WHERE g.userId = :userId " +
           "AND g.createdAt >= :since AND g.status IN ('ACCEPTED', 'REJECTED', 'EDITED')")
    List<SpendingGuess> findFeedbackGuesses(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    /**
     * Calculate acceptance rate for a user
     */
    @Query("SELECT CAST(SUM(CASE WHEN g.status IN ('ACCEPTED', 'EDITED') THEN 1 ELSE 0 END) AS double) / COUNT(g) " +
           "FROM SpendingGuess g WHERE g.userId = :userId AND g.status != 'PENDING' AND g.status != 'EXPIRED'")
    Double calculateAcceptanceRate(@Param("userId") Long userId);

    /**
     * Find recent guesses with actual spending for fallback predictions.
     * Uses createdAt to filter by date range.
     */
    @Query("SELECT g FROM SpendingGuess g WHERE g.userId = :userId " +
           "AND g.createdAt >= :since AND g.status IN ('ACCEPTED', 'EDITED')")
    List<SpendingGuess> findRecentCompletedGuesses(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);
}
