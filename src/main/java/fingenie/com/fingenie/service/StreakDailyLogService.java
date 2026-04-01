package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.StreakDailyLog;
import fingenie.com.fingenie.dto.StreakDailyLogRequest;
import fingenie.com.fingenie.dto.StreakDailyLogResponse;
import fingenie.com.fingenie.repository.StreakDailyLogRepository;
import org.springframework.stereotype.Service;

@Service
public class StreakDailyLogService {

    private final StreakDailyLogRepository repository;

    public StreakDailyLogService(StreakDailyLogRepository repository) {
        this.repository = repository;
    }

    public StreakDailyLogResponse create(StreakDailyLogRequest request) {
        StreakDailyLog log = new StreakDailyLog();
        log.setAccountId(request.getAccountId());
        log.setLogDate(request.getLogDate());
        log.setHasTransaction(request.getHasTransaction());

        StreakDailyLog saved = repository.save(log);

        StreakDailyLogResponse res = new StreakDailyLogResponse();
        res.setLogId(saved.getId());  // Changed from getLogId() to getId()
        res.setAccountId(saved.getAccountId());
        res.setLogDate(saved.getLogDate());
        res.setHasTransaction(saved.getHasTransaction());
        return res;
    }
}
