package fingenie.com.fingenie.review.controller;

import fingenie.com.fingenie.review.dto.PublicReviewResponse;
import fingenie.com.fingenie.review.dto.ReviewResponse;
import fingenie.com.fingenie.review.dto.ReviewUpsertRequest;
import fingenie.com.fingenie.review.service.ReviewService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Public and user review endpoints")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/public")
    @Operation(summary = "Get public approved reviews")
    public ResponseEntity<List<PublicReviewResponse>> getPublicReviews(
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(reviewService.getPublicReviews(limit));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's own review")
    public ResponseEntity<ReviewResponse> getMyReview() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(reviewService.getMyReview(accountId));
    }

    @PostMapping
    @Operation(summary = "Create current user's review")
    public ResponseEntity<ReviewResponse> createMyReview(@Valid @RequestBody ReviewUpsertRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(reviewService.createMyReview(accountId, request));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's review")
    public ResponseEntity<ReviewResponse> updateMyReview(@Valid @RequestBody ReviewUpsertRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(reviewService.updateMyReview(accountId, request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user's review")
    public ResponseEntity<Map<String, String>> deleteMyReview() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        reviewService.deleteMyReview(accountId);
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }
}

