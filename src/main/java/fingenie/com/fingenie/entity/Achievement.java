package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Achievement definition entity.
 * Contains all available achievements with their requirements.
 */
@Entity
@Table(name = "achievement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon")
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    @Builder.Default
    private AchievementTier tier = AchievementTier.BRONZE;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private AchievementCategory category;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private Integer xpReward = 10;

    @Column(name = "target_value", nullable = false)
    @Builder.Default
    private Integer targetValue = 1;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    public enum AchievementTier {
        BRONZE(10),
        SILVER(25),
        GOLD(50),
        PLATINUM(100),
        DIAMOND(200);

        private final int baseXp;

        AchievementTier(int baseXp) {
            this.baseXp = baseXp;
        }

        public int getBaseXp() {
            return baseXp;
        }
    }

    public enum AchievementCategory {
        TRANSACTIONS,
        SAVINGS,
        STREAKS,
        AI_GUESS,
        SOCIAL,
        PET,
        MILESTONES
    }
}
