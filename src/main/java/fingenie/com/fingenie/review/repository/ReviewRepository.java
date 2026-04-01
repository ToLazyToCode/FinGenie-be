package fingenie.com.fingenie.review.repository;

import fingenie.com.fingenie.review.entity.Review;
import fingenie.com.fingenie.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByAccountIdAndIsDeletedFalse(Long accountId);

    List<Review> findByStatusAndIsDeletedFalseAndIsActiveTrueOrderByFeaturedDescUpdatedAtDesc(
            ReviewStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM Review r
            WHERE r.isDeleted = false
              AND (:status IS NULL OR r.status = :status)
              AND (:featured IS NULL OR r.featured = :featured)
            ORDER BY r.featured DESC, r.updatedAt DESC
            """)
    Page<Review> findForAdmin(
            @Param("status") ReviewStatus status,
            @Param("featured") Boolean featured,
            Pageable pageable
    );

    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(ReviewStatus status);

    long countByFeaturedTrueAndStatusAndIsDeletedFalse(ReviewStatus status);

    @Query("""
            SELECT AVG(r.rating)
            FROM Review r
            WHERE r.isDeleted = false
              AND r.status = :status
            """)
    Double averageRatingByStatus(@Param("status") ReviewStatus status);
}
