package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.RiskCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskCatalogRepository extends JpaRepository<RiskCatalog, Long> {

    /**
     * Find risk by code
     */
    Optional<RiskCatalog> findByRiskCode(String riskCode);

    /**
     * Find all open risks ordered by score (highest first)
     */
    List<RiskCatalog> findByStatusOrderByRiskScoreDesc(String status);

    /**
     * Find top N highest risks
     */
    @Query("SELECT r FROM RiskCatalog r WHERE r.status != 'CLOSED' ORDER BY r.riskScore DESC")
    List<RiskCatalog> findTopRisks();

    /**
     * Find risks by category
     */
    List<RiskCatalog> findByCategoryOrderByRiskScoreDesc(String category);

    /**
     * Find risks by priority
     */
    List<RiskCatalog> findByPriorityOrderByRiskScoreDesc(String priority);

    /**
     * Find risks needing review (past review date)
     */
    @Query("SELECT r FROM RiskCatalog r WHERE r.status != 'CLOSED' " +
           "AND (r.lastReviewedAt IS NULL OR r.lastReviewedAt < :reviewThreshold)")
    List<RiskCatalog> findRisksNeedingReview(@Param("reviewThreshold") java.time.LocalDateTime threshold);

    /**
     * Find risks by owner
     */
    List<RiskCatalog> findByOwnerOrderByRiskScoreDesc(String owner);

    /**
     * Count risks by status
     */
    @Query("SELECT r.status, COUNT(r) FROM RiskCatalog r GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * Count risks by priority
     */
    @Query("SELECT r.priority, COUNT(r) FROM RiskCatalog r WHERE r.status != 'CLOSED' GROUP BY r.priority")
    List<Object[]> countByPriority();

    /**
     * Find critical and high priority open risks
     */
    @Query("SELECT r FROM RiskCatalog r WHERE r.status = 'OPEN' " +
           "AND r.priority IN ('CRITICAL', 'HIGH') ORDER BY r.riskScore DESC")
    List<RiskCatalog> findCriticalAndHighRisks();

    /**
     * Calculate average risk score by category
     */
    @Query("SELECT r.category, AVG(r.riskScore) FROM RiskCatalog r " +
           "WHERE r.status != 'CLOSED' GROUP BY r.category")
    List<Object[]> avgRiskScoreByCategory();
}
