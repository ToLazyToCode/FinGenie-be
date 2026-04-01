package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "prediction_feedback")
@Getter
@Setter
public class PredictionFeedback extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "spending_guess_id", nullable = false)
    private Long spendingGuessId;

    @Column(name = "feedback_type", nullable = false, length = 20)
    private String feedbackType; // ACCEPT, EDIT, REJECT

    @Column(name = "original_amount", precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "final_amount", precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "original_category", length = 100)
    private String originalCategory;

    @Column(name = "final_category", length = 100)
    private String finalCategory;

    @Column(name = "feedback_weight", nullable = false, precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 1.00")
    private BigDecimal feedbackWeight = BigDecimal.ONE;

    @Column(name = "comment", length = 500)
    private String comment;
}
