package fingenie.com.fingenie.entity;

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
        name = "reward_catalog_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reward_catalog_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_reward_catalog_kind_active", columnList = "kind, is_active"),
                @Index(name = "idx_reward_catalog_category", columnList = "category")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardCatalogItem extends BaseEntity {

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 40)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private Category category;

    @Column(name = "point_cost")
    private Long pointCost;

    @Column(name = "goal_theme_tags", length = 400)
    private String goalThemeTags;

    @Column(name = "partner_name", length = 160)
    private String partnerName;

    @Column(name = "partner_metadata", length = 500)
    private String partnerMetadata;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "terms_url", length = 500)
    private String termsUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    public enum Kind {
        PERSONAL_VOUCHER,
        SHARED_PIGGY_GROUP_REWARD
    }

    public enum Category {
        TRAVEL,
        ELECTRONICS,
        ESSENTIALS,
        EDUCATION,
        LIFESTYLE
    }
}
