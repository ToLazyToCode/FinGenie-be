package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    List<PromptTemplate> findByCategoryAndIsActiveTrue(String category);
}
