package fingenie.com.fingenie.survey.repository;

import fingenie.com.fingenie.survey.entity.SurveyQuestion;
import fingenie.com.fingenie.survey.enums.SurveySection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {
    
    List<SurveyQuestion> findBySurveyDefinitionIdOrderByQuestionOrderAsc(Long surveyDefinitionId);
    
    List<SurveyQuestion> findBySurveyDefinitionIdAndSectionCodeOrderByQuestionOrderAsc(
            Long surveyDefinitionId, SurveySection sectionCode);
    
    Optional<SurveyQuestion> findByQuestionCode(String questionCode);
    
    @Query("SELECT DISTINCT sq FROM SurveyQuestion sq LEFT JOIN FETCH sq.answerOptions " +
           "WHERE sq.surveyDefinition.id = :surveyId ORDER BY sq.questionOrder ASC")
    List<SurveyQuestion> findAllWithAnswerOptions(@Param("surveyId") Long surveyId);
    
    @Query("SELECT COUNT(sq) FROM SurveyQuestion sq WHERE sq.surveyDefinition.id = :surveyId AND sq.isRequired = true")
    int countRequiredQuestions(@Param("surveyId") Long surveyId);
}
