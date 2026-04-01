package fingenie.com.fingenie.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewSummaryResponse {
    private long totalReviews;
    private long pendingReviews;
    private long approvedReviews;
    private long rejectedReviews;
    private long hiddenReviews;
    private long featuredReviews;
    private double averageRating;
}

