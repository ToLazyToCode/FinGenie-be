package fingenie.com.fingenie.survey.repository;

import fingenie.com.fingenie.survey.entity.SurveyDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyDefinitionRepository extends JpaRepository<SurveyDefinition, Long> {

    Optional<SurveyDefinition> findByVersion(Long version);

    Optional<SurveyDefinition> findFirstByIsActiveSurveyTrueOrderByCreatedAtDesc();

    default Optional<SurveyDefinition> findActiveSurvey() {
        return findFirstByIsActiveSurveyTrueOrderByCreatedAtDesc();
    }

    default Optional<SurveyDefinition> findActiveSurveyWithQuestionsAndOptions() {
        return findActiveSurvey();
    }

    boolean existsByVersion(Long version);
}
