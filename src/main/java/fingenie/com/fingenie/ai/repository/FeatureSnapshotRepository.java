package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.FeatureSnapshot;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureSnapshotRepository extends JpaRepository<FeatureSnapshot, Long> {

    /**
     * Find latest snapshot for a user
     */
    Optional<FeatureSnapshot> findFirstByUserIdOrderBySnapshotDateDesc(Long userId);

    /**
     * Find snapshot by user and date
     */
    Optional<FeatureSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    /**
     * Find snapshot by hash for reproducibility
     */
    Optional<FeatureSnapshot> findByFeatureHash(String featureHash);

    /**
     * Find snapshots for a user within date range (for training)
     */
    List<FeatureSnapshot> findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all snapshots for a date (batch processing)
     */
    List<FeatureSnapshot> findBySnapshotDate(LocalDate snapshotDate);

    /**
     * Delete old snapshots (retention policy)
     */
    @Modifying
    @Query("DELETE FROM FeatureSnapshot f WHERE f.snapshotDate < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Count snapshots by feature version
     */
    @Query("SELECT f.featureVersion, COUNT(f) FROM FeatureSnapshot f GROUP BY f.featureVersion")
    List<Object[]> countByFeatureVersion();

    /**
     * Find users with missing snapshots for a date
     */
    @Query("SELECT DISTINCT a.id FROM Account a " +
           "WHERE a.isDeleted = false AND a.isActive = true " +
           "AND a.id NOT IN (SELECT f.userId FROM FeatureSnapshot f WHERE f.snapshotDate = :date)")
    List<Long> findUsersWithMissingSnapshot(@Param("date") LocalDate date);
}
