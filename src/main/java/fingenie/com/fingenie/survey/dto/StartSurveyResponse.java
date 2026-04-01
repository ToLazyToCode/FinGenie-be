package fingenie.com.fingenie.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO after starting a survey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSurveyResponse {
    private Long responseId;
    private Long surveyId;
    private String surveyVersion;
    private Integer responseVersion;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private String status;
    private Map<String, String> existingAnswers; // For resuming in-progress surveys
}
