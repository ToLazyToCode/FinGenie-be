package fingenie.com.fingenie.survey.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for submitting survey answers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitSurveyRequest {
    
    @NotEmpty(message = "Answers cannot be empty")
    private Map<String, String> answers; // question_code -> answer_code
    
    private Boolean isPartialSubmission; // true = save progress, false = final submission
}
