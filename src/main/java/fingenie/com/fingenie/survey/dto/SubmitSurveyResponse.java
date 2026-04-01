package fingenie.com.fingenie.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO after submitting survey answers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitSurveyResponse {
    private Long responseId;
    private String status;
    private Integer answeredQuestions;
    private Integer totalQuestions;
    private Boolean isComplete;
    private LocalDateTime completedAt;
    private String message;
}
