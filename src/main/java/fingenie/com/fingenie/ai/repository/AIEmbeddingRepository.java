package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.AIEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIEmbeddingRepository extends JpaRepository<AIEmbedding, Long> {
    List<AIEmbedding> findByUserId(Long userId);
}
