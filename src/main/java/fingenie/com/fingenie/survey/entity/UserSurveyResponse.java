package fingenie.com.fingenie.survey.entity;

import fingenie.com.fingenie.base.BaseEntity;
import fingenie.com.fingenie.survey.enums.SurveyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * User Survey Response - Immutable record of user's survey answers.
 * 
 * Immutability rules:
 * - No updates to answers after submission
 * - New survey = new response_version
 * - Previous responses marked as is_latest = false
 */
@Entity
@Table(name = "user_survey_response",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "survey_definition_id", "response_version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSurveyResponse extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_definition_id", nullable = false)
    private SurveyDefinition surveyDefinition;
    
    @Column(name = "response_version", nullable = false)
    @Builder.Default
    private Integer responseVersion = 1;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SurveyStatus status = SurveyStatus.IN_PROGRESS;
    
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "consent_given", nullable = false)
    @Builder.Default
    private Boolean consentGiven = false;
    
    @Column(name = "consent_timestamp")
    private LocalDateTime consentTimestamp;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> answers = new HashMap<>();
    
    @Column(name = "is_latest", nullable = false)
    @Builder.Default
    private Boolean isLatest = true;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * Set a single answer. Used for progress saving.
     */
    public void setAnswer(String questionCode, String answerCode) {
        if (this.answers == null) {
            this.answers = new HashMap<>();
        }
        this.answers.put(questionCode, answerCode);
    }
    
    /**
     * Check if survey is complete (all required questions answered).
     */
    public boolean isComplete(int requiredQuestionCount) {
        return answers != null && answers.size() >= requiredQuestionCount;
    }
    
    /**
     * Mark survey as completed.
     */
    public void markCompleted() {
        this.status = SurveyStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Mark survey as expired.
     */
    public void markExpired() {
        this.status = SurveyStatus.EXPIRED;
    }
    
    /**
     * Mark as superseded when user starts a new survey.
     */
    public void markSuperseded() {
        this.status = SurveyStatus.SUPERSEDED;
        this.isLatest = false;
    }
}

