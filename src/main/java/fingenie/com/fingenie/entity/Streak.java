package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "streak")
@Getter
@Setter
public class Streak extends BaseEntity { 

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "current_streak", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer longestStreak = 0;

    @Column(name = "last_active_date", length = 10)
    private String lastActiveDate;
}