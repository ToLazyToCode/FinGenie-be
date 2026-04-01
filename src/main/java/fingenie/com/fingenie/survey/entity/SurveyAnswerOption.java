package fingenie.com.fingenie.survey.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Survey Answer Option - Predefined answer choices for questions.
 */
@Entity
@Table(name = "survey_answer_option", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "answer_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyAnswerOption extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;
    
    @Column(name = "answer_code", nullable = false, length = 50)
    private String answerCode;
    
    @Column(name = "answer_text", nullable = false)
    private String answerText;
    
    @Column(name = "score_value", nullable = false)
    private Integer scoreValue;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
