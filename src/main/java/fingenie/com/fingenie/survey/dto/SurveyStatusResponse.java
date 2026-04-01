package fingenie.com.fingenie.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for survey status check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyStatusResponse {
    private Long userId;
    private Boolean hasSurvey;
    private Boolean hasCompletedSurvey;
    private String currentStatus;
    private Integer completedVersion;
    private LocalDateTime completedAt;
    private Boolean canRetake;
    private Integer daysSinceCompletion;
    private String message;
}
