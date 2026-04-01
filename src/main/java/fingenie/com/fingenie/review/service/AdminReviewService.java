package fingenie.com.fingenie.review.service;

import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.review.dto.AdminReviewResponse;
import fingenie.com.fingenie.review.dto.AdminReviewSummaryResponse;
import fingenie.com.fingenie.review.entity.Review;
import fingenie.com.fingenie.review.entity.ReviewStatus;
import fingenie.com.fingenie.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public AdminReviewSummaryResponse getSummary() {
        long totalReviews = reviewRepository.countByIsDeletedFalse();
        long pendingReviews = reviewRepository.countByStatusAndIsDeletedFalse(ReviewStatus.PENDING);
        long approvedReviews = reviewRepository.countByStatusAndIsDeletedFalse(ReviewStatus.APPROVED);
        long rejectedReviews = reviewRepository.countByStatusAndIsDeletedFalse(ReviewStatus.REJECTED);
        long hiddenReviews = reviewRepository.countByStatusAndIsDeletedFalse(ReviewStatus.HIDDEN);
        long featuredReviews = reviewRepository.countByFeaturedTrueAndStatusAndIsDeletedFalse(ReviewStatus.APPROVED);

        Double avgRatingRaw = reviewRepository.averageRatingByStatus(ReviewStatus.APPROVED);
        double averageRating = avgRatingRaw == null ? 0.0 : round(avgRatingRaw);

        return AdminReviewSummaryResponse.builder()
                .totalReviews(totalReviews)
                .pendingReviews(pendingReviews)
                .approvedReviews(approvedReviews)
                .rejectedReviews(rejectedReviews)
                .hiddenReviews(hiddenReviews)
                .featuredReviews(featuredReviews)
                .averageRating(averageRating)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getReviews(int page, int size, ReviewStatus status, Boolean featured) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("updatedAt"))
        );

        return reviewRepository.findForAdmin(status, featured, pageable)
                .map(this::toAdminReviewResponse);
    }

    @Transactional
    public AdminReviewResponse approve(Long reviewId, Long adminAccountId) {
        Review review = getReviewOrThrow(reviewId);
        review.setStatus(ReviewStatus.APPROVED);
        markModerated(review, adminAccountId, null);
        return toAdminReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public AdminReviewResponse reject(Long reviewId, Long adminAccountId, String reason) {
        Review review = getReviewOrThrow(reviewId);
        review.setStatus(ReviewStatus.REJECTED);
        review.setFeatured(false);
        markModerated(review, adminAccountId, normalizeReason(reason, "Rejected by admin"));
        return toAdminReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public AdminReviewResponse feature(Long reviewId, Long adminAccountId) {
        Review review = getReviewOrThrow(reviewId);
        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "reviewId", reviewId,
                    "reason", "Only APPROVED reviews can be featured."
            ));
        }
        review.setFeatured(true);
        markModerated(review, adminAccountId, review.getModerationNote());
        return toAdminReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public AdminReviewResponse unfeature(Long reviewId, Long adminAccountId) {
        Review review = getReviewOrThrow(reviewId);
        review.setFeatured(false);
        markModerated(review, adminAccountId, review.getModerationNote());
        return toAdminReviewResponse(reviewRepository.save(review));
    }

    private Review getReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .filter(review -> !review.isDeleted())
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "reviewId", "notFound",
                        "value", reviewId
                )));
    }

    private void markModerated(Review review, Long adminAccountId, String moderationNote) {
        review.setModeratedByAccountId(adminAccountId);
        review.setModeratedAt(Timestamp.from(Instant.now()));
        review.setModerationNote(moderationNote);
    }

    private String normalizeReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }
        String trimmed = reason.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private AdminReviewResponse toAdminReviewResponse(Review review) {
        return AdminReviewResponse.builder()
                .id(review.getId())
                .accountId(review.getAccountId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .status(review.getStatus())
                .featured(review.isFeatured())
                .displayNameSnapshot(review.getDisplayNameSnapshot())
                .moderatedByAccountId(review.getModeratedByAccountId())
                .moderatedAt(review.getModeratedAt())
                .moderationNote(review.getModerationNote())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}

