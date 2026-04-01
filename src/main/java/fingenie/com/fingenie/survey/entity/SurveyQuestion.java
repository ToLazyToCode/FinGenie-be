package fingenie.com.fingenie.survey.entity;

import fingenie.com.fingenie.base.BaseEntity;
import fingenie.com.fingenie.survey.enums.SurveySection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Survey Question - Individual questions within a survey.
 */
@Entity
@Table(name = "survey_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyQuestion extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_definition_id", nullable = false)
    private SurveyDefinition surveyDefinition;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "section_code", nullable = false, length = 10)
    private SurveySection sectionCode;
    
    @Column(name = "question_code", nullable = false, unique = true, length = 10)
    private String questionCode;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;
    
    @Column(name = "weight", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;
    
    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;
    
    @Column(name = "answer_enum_class")
    private String answerEnumClass;
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<SurveyAnswerOption> answerOptions = new ArrayList<>();
    
    public void addAnswerOption(SurveyAnswerOption option) {
        answerOptions.add(option);
        option.setQuestion(this);
    }
}
