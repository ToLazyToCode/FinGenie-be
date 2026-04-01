package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.PredictionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PredictionFeedbackRepository extends JpaRepository<PredictionFeedback, Long> {
    List<PredictionFeedback> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<PredictionFeedback> findByUserIdAndCreatedAtAfter(Long userId, Timestamp createdAt);
    
    @Query("SELECT COUNT(pf) FROM PredictionFeedback pf WHERE pf.userId = :userId AND pf.feedbackType = :type")
    Long countByUserIdAndFeedbackType(@Param("userId") Long userId, @Param("type") String feedbackType);
    
    @Query("SELECT pf FROM PredictionFeedback pf WHERE pf.userId = :userId ORDER BY pf.createdAt DESC")
    List<PredictionFeedback> findRecentFeedback(@Param("userId") Long userId);
}
