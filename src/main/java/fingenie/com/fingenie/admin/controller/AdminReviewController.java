package fingenie.com.fingenie.admin.controller;

import fingenie.com.fingenie.review.dto.AdminReviewModerationRequest;
import fingenie.com.fingenie.review.dto.AdminReviewResponse;
import fingenie.com.fingenie.review.dto.AdminReviewSummaryResponse;
import fingenie.com.fingenie.review.entity.ReviewStatus;
import fingenie.com.fingenie.review.service.AdminReviewService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api-prefix}/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Reviews", description = "Review moderation and review analytics for admins")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping("/summary")
    @Operation(summary = "Get review KPI summary")
    public ResponseEntity<AdminReviewSummaryResponse> getSummary() {
        return ResponseEntity.ok(adminReviewService.getSummary());
    }

    @GetMapping
    @Operation(summary = "Get paginated reviews for moderation")
    public ResponseEntity<Page<AdminReviewResponse>> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Boolean featured
    ) {
        return ResponseEntity.ok(adminReviewService.getReviews(page, size, status, featured));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a review")
    public ResponseEntity<AdminReviewResponse> approveReview(@PathVariable Long id) {
        Long adminAccountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(adminReviewService.approve(id, adminAccountId));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a review")
    public ResponseEntity<AdminReviewResponse> rejectReview(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid AdminReviewModerationRequest request
    ) {
        Long adminAccountId = SecurityUtils.getCurrentAccountId();
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(adminReviewService.reject(id, adminAccountId, reason));
    }

    @PostMapping("/{id}/feature")
    @Operation(summary = "Feature an approved review")
    public ResponseEntity<AdminReviewResponse> featureReview(@PathVariable Long id) {
        Long adminAccountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(adminReviewService.feature(id, adminAccountId));
    }

    @PostMapping("/{id}/unfeature")
    @Operation(summary = "Unfeature a review")
    public ResponseEntity<AdminReviewResponse> unfeatureReview(@PathVariable Long id) {
        Long adminAccountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(adminReviewService.unfeature(id, adminAccountId));
    }
}

