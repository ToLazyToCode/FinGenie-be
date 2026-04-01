package fingenie.com.fingenie.survey.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO to start a new survey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSurveyRequest {
    
    @NotNull(message = "Consent is required to start the survey")
    private Boolean consentGiven;
    
    private String surveyVersion; // Optional: use specific version, or null for active survey
}
