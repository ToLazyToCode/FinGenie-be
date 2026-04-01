package fingenie.com.fingenie.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicReviewResponse {
    private Long id;
    private Integer rating;
    private String title;
    private String comment;
    private boolean featured;
    private String displayNameSnapshot;
    private Timestamp createdAt;
}

