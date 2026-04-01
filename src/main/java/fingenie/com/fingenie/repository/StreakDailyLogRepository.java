package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.StreakDailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StreakDailyLogRepository
        extends JpaRepository<StreakDailyLog, Long> {
        
    Optional<StreakDailyLog> findByAccountIdAndLogDate(Long accountId, LocalDate logDate);
}
