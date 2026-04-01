package fingenie.com.fingenie.review.dto;

import fingenie.com.fingenie.review.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long accountId;
    private Integer rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private boolean featured;
    private String displayNameSnapshot;
    private Long moderatedByAccountId;
    private Timestamp moderatedAt;
    private String moderationNote;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}

