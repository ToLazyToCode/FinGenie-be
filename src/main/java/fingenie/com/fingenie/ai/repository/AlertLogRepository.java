package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.AlertLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    /**
     * Find open alerts ordered by severity
     */
    List<AlertLog> findByStatusOrderBySeverityLevelDescTriggeredAtDesc(String status);

    /**
     * Find alerts by type
     */
    List<AlertLog> findByAlertTypeOrderByTriggeredAtDesc(String alertType);

    /**
     * Find alerts by severity level
     */
    List<AlertLog> findBySeverityLevelOrderByTriggeredAtDesc(Integer severityLevel);

    /**
     * Find recent alerts
     */
    Page<AlertLog> findByTriggeredAtAfterOrderByTriggeredAtDesc(LocalDateTime since, Pageable pageable);

    /**
     * Find unresolved alerts within time window
     */
    @Query("SELECT a FROM AlertLog a WHERE a.status NOT IN ('RESOLVED', 'FALSE_POSITIVE') " +
           "AND a.triggeredAt > :since ORDER BY a.severityLevel DESC, a.triggeredAt DESC")
    List<AlertLog> findActiveAlerts(@Param("since") LocalDateTime since);

    /**
     * Count alerts by type within time window
     */
    @Query("SELECT a.alertType, COUNT(a) FROM AlertLog a WHERE a.triggeredAt > :since GROUP BY a.alertType")
    List<Object[]> countByAlertType(@Param("since") LocalDateTime since);

    /**
     * Count alerts by severity
     */
    @Query("SELECT a.severityLevel, COUNT(a) FROM AlertLog a WHERE a.triggeredAt > :since GROUP BY a.severityLevel")
    List<Object[]> countBySeverity(@Param("since") LocalDateTime since);

    /**
     * Find escalated unresolved alerts
     */
    @Query("SELECT a FROM AlertLog a WHERE a.escalated = true " +
           "AND a.status NOT IN ('RESOLVED', 'FALSE_POSITIVE') ORDER BY a.triggeredAt DESC")
    List<AlertLog> findEscalatedUnresolved();

    /**
     * Calculate MTTR (Mean Time To Resolution) in minutes
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, a.triggeredAt, a.resolvedAt)) FROM AlertLog a " +
           "WHERE a.resolvedAt IS NOT NULL AND a.triggeredAt > :since")
    Double calculateMTTR(@Param("since") LocalDateTime since);

    /**
     * Find related alerts by risk code
     */
    List<AlertLog> findByRelatedRiskCodeOrderByTriggeredAtDesc(String riskCode);

    /**
     * Find by correlation ID for tracing
     */
    List<AlertLog> findByCorrelationId(String correlationId);

    /**
     * Find alerts with auto-remediation applied
     */
    @Query("SELECT a FROM AlertLog a WHERE a.autoRemediationApplied = true " +
           "AND a.triggeredAt > :since ORDER BY a.triggeredAt DESC")
    List<AlertLog> findAutoRemediatedAlerts(@Param("since") LocalDateTime since);

    /**
     * Count open alerts by severity (for dashboard)
     */
    @Query("SELECT a.severityLevel, COUNT(a) FROM AlertLog a " +
           "WHERE a.status NOT IN ('RESOLVED', 'FALSE_POSITIVE') GROUP BY a.severityLevel")
    List<Object[]> countOpenAlertsBySeverity();
}
