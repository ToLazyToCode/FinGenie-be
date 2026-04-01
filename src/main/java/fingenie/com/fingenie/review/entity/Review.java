package fingenie.com.fingenie.review.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(
        name = "review",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_account", columnNames = "account_id")
        },
        indexes = {
                @Index(name = "idx_review_status", columnList = "status"),
                @Index(name = "idx_review_featured", columnList = "featured"),
                @Index(name = "idx_review_updated_at", columnList = "updated_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "title", length = 120)
    private String title;

    @Column(name = "comment_text", nullable = false, length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "display_name_snapshot", nullable = false, length = 150)
    private String displayNameSnapshot;

    @Column(name = "moderated_by_account_id")
    private Long moderatedByAccountId;

    @Column(name = "moderated_at")
    private Timestamp moderatedAt;

    @Column(name = "moderation_note", length = 500)
    private String moderationNote;
}

