package fingenie.com.fingenie.review.service;

import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.UserProfile;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.UserProfileRepository;
import fingenie.com.fingenie.review.dto.PublicReviewResponse;
import fingenie.com.fingenie.review.dto.ReviewResponse;
import fingenie.com.fingenie.review.dto.ReviewUpsertRequest;
import fingenie.com.fingenie.review.entity.Review;
import fingenie.com.fingenie.review.entity.ReviewStatus;
import fingenie.com.fingenie.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int DEFAULT_PUBLIC_LIMIT = 12;
    private static final int MAX_PUBLIC_LIMIT = 50;

    private final ReviewRepository reviewRepository;
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public List<PublicReviewResponse> getPublicReviews(Integer limit) {
        int safeLimit = sanitizeLimit(limit);
        Pageable pageable = PageRequest.of(
                0,
                safeLimit,
                Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("updatedAt"))
        );

        return reviewRepository.findByStatusAndIsDeletedFalseAndIsActiveTrueOrderByFeaturedDescUpdatedAtDesc(
                        ReviewStatus.APPROVED,
                        pageable
                ).stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(Long accountId) {
        return reviewRepository.findByAccountIdAndIsDeletedFalse(accountId)
                .map(this::toReviewResponse)
                .orElse(null);
    }

    @Transactional
    public ReviewResponse createMyReview(Long accountId, ReviewUpsertRequest request) {
        if (reviewRepository.findByAccountIdAndIsDeletedFalse(accountId).isPresent()) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "review", "alreadyExists",
                    "hint", "Use PUT /api/v1/reviews/me to update the existing review."
            ));
        }

        Review review = Review.builder()
                .accountId(accountId)
                .rating(request.getRating())
                .title(normalizeTitle(request.getTitle()))
                .comment(normalizeComment(request.getComment()))
                .status(ReviewStatus.PENDING)
                .featured(false)
                .displayNameSnapshot(resolveDisplayName(accountId))
                .build();

        return toReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateMyReview(Long accountId, ReviewUpsertRequest request) {
        Review review = reviewRepository.findByAccountIdAndIsDeletedFalse(accountId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "review", "notFound"
                )));

        String normalizedTitle = normalizeTitle(request.getTitle());
        String normalizedComment = normalizeComment(request.getComment());
        boolean hasMeaningfulChange = !equalsSafe(review.getTitle(), normalizedTitle)
                || !equalsSafe(review.getComment(), normalizedComment)
                || !equalsSafe(review.getRating(), request.getRating());

        review.setRating(request.getRating());
        review.setTitle(normalizedTitle);
        review.setComment(normalizedComment);
        review.setDisplayNameSnapshot(resolveDisplayName(accountId));

        // Any user edit re-enters moderation for MVP.
        if (hasMeaningfulChange) {
            review.setStatus(ReviewStatus.PENDING);
            review.setFeatured(false);
            review.setModeratedByAccountId(null);
            review.setModeratedAt(null);
            review.setModerationNote(null);
        }

        return toReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public void deleteMyReview(Long accountId) {
        Review review = reviewRepository.findByAccountIdAndIsDeletedFalse(accountId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "review", "notFound"
                )));
        reviewRepository.delete(review);
    }

    private PublicReviewResponse toPublicResponse(Review review) {
        return PublicReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .featured(review.isFeatured())
                .displayNameSnapshot(review.getDisplayNameSnapshot())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.builder()
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

    private String resolveDisplayName(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new SystemExceptions.ValidationException(Map.of(
                        "accountId", "notFound",
                        "value", accountId
                )));

        UserProfile profile = userProfileRepository.findByAccountId(accountId).orElse(null);
        if (profile != null && !isBlank(profile.getFullName())) {
            return normalizeDisplayName(profile.getFullName());
        }
        if (!isBlank(account.getName())) {
            return normalizeDisplayName(account.getName());
        }

        String email = account.getEmail();
        if (!isBlank(email)) {
            int atIndex = email.indexOf('@');
            String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
            if (!isBlank(localPart)) {
                return normalizeDisplayName(localPart);
            }
        }
        return "FinGenie User";
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_PUBLIC_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_PUBLIC_LIMIT);
    }

    private String normalizeTitle(String input) {
        if (isBlank(input)) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    private String normalizeComment(String input) {
        if (isBlank(input)) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "comment", "required"
            ));
        }
        String trimmed = input.trim();
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed;
    }

    private String normalizeDisplayName(String input) {
        String trimmed = input.trim();
        return trimmed.length() > 150 ? trimmed.substring(0, 150) : trimmed;
    }

    private boolean equalsSafe(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
