package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.DriftMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriftMetricsRepository extends JpaRepository<DriftMetrics, Long> {

    /**
     * Find latest drift metrics for a model
     */
    Optional<DriftMetrics> findFirstByModelVersionOrderByMetricDateDesc(String modelVersion);

    /**
     * Find drift metrics for a date
     */
    List<DriftMetrics> findByMetricDate(LocalDate metricDate);

    /**
     * Find drift metrics for a model within date range
     */
    List<DriftMetrics> findByModelVersionAndMetricDateBetweenOrderByMetricDateAsc(
            String modelVersion, LocalDate startDate, LocalDate endDate);

    /**
     * Find metrics where alert was triggered
     */
    List<DriftMetrics> findByAlertTriggeredTrueOrderByMetricDateDesc();

    /**
     * Find metrics requiring retraining
     */
    @Query("SELECT d FROM DriftMetrics d WHERE d.requiresRetraining = true " +
           "AND d.retrainingScheduledAt IS NULL ORDER BY d.metricDate DESC")
    List<DriftMetrics> findPendingRetraining();

    /**
     * Get trend data for a model (last N days)
     */
    @Query("SELECT d FROM DriftMetrics d WHERE d.modelVersion = :modelVersion " +
           "AND d.metricDate >= :startDate ORDER BY d.metricDate ASC")
    List<DriftMetrics> getDriftTrend(
            @Param("modelVersion") String modelVersion,
            @Param("startDate") LocalDate startDate);

    /**
     * Find by drift type
     */
    List<DriftMetrics> findByDriftTypeAndMetricDateBetweenOrderByMetricDateDesc(
            String driftType, LocalDate startDate, LocalDate endDate);

    /**
     * Calculate average KL divergence over period
     */
    @Query("SELECT AVG(d.klDivergence) FROM DriftMetrics d " +
           "WHERE d.modelVersion = :modelVersion AND d.metricDate >= :startDate")
    Double avgKlDivergence(
            @Param("modelVersion") String modelVersion,
            @Param("startDate") LocalDate startDate);

    /**
     * Find critical drift alerts (for incident management)
     */
    @Query("SELECT d FROM DriftMetrics d WHERE d.alertLevel = 'CRITICAL' " +
           "AND d.metricDate >= :since ORDER BY d.metricDate DESC")
    List<DriftMetrics> findCriticalDriftAlerts(@Param("since") LocalDate since);

    /**
     * Count alerts by level within period
     */
    @Query("SELECT d.alertLevel, COUNT(d) FROM DriftMetrics d " +
           "WHERE d.alertTriggered = true AND d.metricDate >= :since GROUP BY d.alertLevel")
    List<Object[]> countAlertsByLevel(@Param("since") LocalDate since);
}
