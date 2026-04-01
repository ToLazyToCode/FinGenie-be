package fingenie.com.fingenie.survey.repository;

import fingenie.com.fingenie.survey.entity.SurveyAnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyAnswerOptionRepository extends JpaRepository<SurveyAnswerOption, Long> {
    
    List<SurveyAnswerOption> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);
    
    Optional<SurveyAnswerOption> findByQuestionIdAndAnswerCode(Long questionId, String answerCode);
    
    List<SurveyAnswerOption> findByQuestionQuestionCode(String questionCode);
}
