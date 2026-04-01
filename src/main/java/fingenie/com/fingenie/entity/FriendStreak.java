package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "friend_streak")
@Getter
@Setter
public class FriendStreak extends BaseEntity {

    @Column(name = "friendship_id", nullable = false)
    private Long friendshipId;

    @Column(name = "current_streak", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer currentStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;
}
