package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.ModelRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, Long> {

    /**
     * Find the current production model by name
     */
    Optional<ModelRegistry> findByModelNameAndStatus(String modelName, String status);

    /**
     * Find production model for a type
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelType = :modelType AND m.status = 'PRODUCTION'")
    Optional<ModelRegistry> findProductionModelByType(@Param("modelType") String modelType);

    /**
     * Find all models by name ordered by version
     */
    List<ModelRegistry> findByModelNameOrderByCreatedAtDesc(String modelName);

    /**
     * Find by MLflow run ID
     */
    Optional<ModelRegistry> findByMlflowRunId(String mlflowRunId);

    /**
     * Find by version
     */
    Optional<ModelRegistry> findByModelNameAndModelVersion(String modelName, String modelVersion);

    /**
     * Archive old models (keep only latest N versions)
     */
    @Modifying
    @Query("UPDATE ModelRegistry m SET m.status = 'ARCHIVED' " +
           "WHERE m.modelName = :modelName AND m.status != 'PRODUCTION' " +
           "AND m.id NOT IN (SELECT m2.id FROM ModelRegistry m2 WHERE m2.modelName = :modelName " +
           "ORDER BY m2.createdAt DESC LIMIT :keepVersions)")
    int archiveOldVersions(@Param("modelName") String modelName, @Param("keepVersions") int keepVersions);

    /**
     * Find models in shadow testing
     */
    List<ModelRegistry> findByStatus(String status);

    /**
     * Count models by status
     */
    @Query("SELECT m.status, COUNT(m) FROM ModelRegistry m GROUP BY m.status")
    List<Object[]> countByStatus();

    /**
     * Find rollback candidates (previous production versions)
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelName = :modelName " +
           "AND m.status = 'ARCHIVED' AND m.deployedAt IS NOT NULL " +
           "ORDER BY m.deployedAt DESC")
    List<ModelRegistry> findRollbackCandidates(@Param("modelName") String modelName);
}
