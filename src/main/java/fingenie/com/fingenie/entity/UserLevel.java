package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_level")
@Getter
@Setter
public class UserLevel extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "current_level", nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer currentLevel = 1;

    @Column(name = "current_xp", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer currentXp = 0;

    @Column(name = "lifetime_xp", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer lifetimeXp = 0;
}
