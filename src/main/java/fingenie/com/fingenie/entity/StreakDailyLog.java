package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "streak_daily_log")
@Getter
@Setter
public class StreakDailyLog extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "has_transaction", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean hasTransaction = false;
}
