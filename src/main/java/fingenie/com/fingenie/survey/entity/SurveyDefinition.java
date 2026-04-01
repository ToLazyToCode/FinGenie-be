package fingenie.com.fingenie.survey.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Survey Definition - Versioned survey templates.
 * 
 * Allows different versions of surveys to coexist for:
 * - A/B testing
 * - Gradual rollout
 * - Preserving historical responses
 */
@Entity
@Table(name = "survey_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyDefinition extends BaseEntity {
    
    @Column(name = "version", nullable = false, unique = true, length = 20)
    private Long version;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active_survey")
    @Builder.Default
    private Boolean isActiveSurvey = true;
    
    @Column(name = "estimated_minutes")
    @Builder.Default
    private Integer estimatedMinutes = 5;
    
    @OneToMany(mappedBy = "surveyDefinition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("questionOrder ASC")
    @Builder.Default
    private List<SurveyQuestion> questions = new ArrayList<>();
    
    public void addQuestion(SurveyQuestion question) {
        questions.add(question);
        question.setSurveyDefinition(this);
    }
}
