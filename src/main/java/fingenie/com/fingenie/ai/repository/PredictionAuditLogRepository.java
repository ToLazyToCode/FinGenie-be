package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.PredictionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface PredictionAuditLogRepository extends JpaRepository<PredictionAuditLog, Long> {

    Page<PredictionAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<PredictionAuditLog> findByModelVersion(String modelVersion);

    List<PredictionAuditLog> findByFallbackUsedTrueAndCreatedAtAfter(Timestamp since);

    @Query("SELECT p.modelVersion, COUNT(p) FROM PredictionAuditLog p GROUP BY p.modelVersion")
    List<Object[]> countByModelVersion();

    @Query("SELECT p.modelVersion, AVG(p.inferenceLatencyMs) FROM PredictionAuditLog p " +
           "WHERE p.inferenceLatencyMs IS NOT NULL GROUP BY p.modelVersion")
    List<Object[]> avgLatencyByModelVersion();

    @Query("SELECT p FROM PredictionAuditLog p WHERE p.inferenceLatencyMs > :threshold " +
           "AND p.createdAt > :since ORDER BY p.createdAt DESC")
    List<PredictionAuditLog> findHighLatencyPredictions(
            @Param("threshold") Integer thresholdMs,
            @Param("since") Timestamp since);

    @Query("SELECT COUNT(p) * 1.0 / (SELECT COUNT(p2) FROM PredictionAuditLog p2 WHERE p2.createdAt > :since) " +
           "FROM PredictionAuditLog p WHERE p.fallbackUsed = true AND p.createdAt > :since")
    Double calculateFallbackRate(@Param("since") Timestamp since);

    List<PredictionAuditLog> findByCorrelationId(String correlationId);

    List<PredictionAuditLog> findByFeatureSnapshotHash(String hash);

    boolean existsByCorrelationIdAndPredictionIdAndUserId(String correlationId, Long predictionId, Long userId);
}
