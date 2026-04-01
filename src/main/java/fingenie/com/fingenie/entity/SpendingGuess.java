package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SpendingGuess - AI-generated spending prediction for users.
 * Users can ACCEPT (auto-create transaction), EDIT, or REJECT guesses.
 */
@Entity
@Table(name = "spending_guess")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingGuess extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "guessed_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal guessedAmount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "VND";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(name = "confidence", precision = 3, scale = 2, nullable = false)
    private BigDecimal confidence;

    @Column(name = "guessed_for_time", nullable = false)
    private LocalDateTime guessedForTime;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED, EXPIRED, EDITED

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "feedback_reason")
    private String feedbackReason;

    // If accepted with edit, store original vs final
    @Column(name = "original_amount", precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "final_amount", precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "original_category_id")
    private Long originalCategoryId;

    @Column(name = "final_category_id")
    private Long finalCategoryId;

    // Link to created transaction if accepted
    @Column(name = "created_transaction_id")
    private Long createdTransactionId;

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAccepted(Long transactionId) {
        this.status = "ACCEPTED";
        this.acceptedAt = LocalDateTime.now();
        this.createdTransactionId = transactionId;
        this.finalAmount = this.guessedAmount;
        this.finalCategoryId = this.category != null ? this.category.getId() : null;
    }

    public void markEdited(BigDecimal newAmount, Long newCategoryId, Long transactionId) {
        this.status = "EDITED";
        this.acceptedAt = LocalDateTime.now();
        this.createdTransactionId = transactionId;
        this.originalAmount = this.guessedAmount;
        this.originalCategoryId = this.category != null ? this.category.getId() : null;
        this.finalAmount = newAmount;
        this.finalCategoryId = newCategoryId;
    }

    public void markRejected(String reason) {
        this.status = "REJECTED";
        this.rejectedAt = LocalDateTime.now();
        this.feedbackReason = reason;
    }

    public void markExpired() {
        this.status = "EXPIRED";
    }
}
