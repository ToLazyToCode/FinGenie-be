package fingenie.com.fingenie.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionFeedbackRequest {
    private Long accountId;
    private Long predictionId;
    private String feedbackType; // ACCEPT / REJECT / EDIT
    private String comment;
}
