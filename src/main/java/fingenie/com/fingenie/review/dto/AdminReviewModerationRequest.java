package fingenie.com.fingenie.review.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewModerationRequest {

    @Size(max = 500)
    private String reason;
}

