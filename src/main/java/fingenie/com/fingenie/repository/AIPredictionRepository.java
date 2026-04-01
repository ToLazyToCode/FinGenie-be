package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.AIPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIPredictionRepository extends JpaRepository<AIPrediction, Long> {
    Optional<AIPrediction> findTopByAccountIdOrderByCreatedAtDesc(Long accountId);
}
