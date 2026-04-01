package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.StreakDailyLogRequest;
import fingenie.com.fingenie.service.StreakDailyLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streak-daily-logs")
public class StreakDailyLogController {

    private final StreakDailyLogService service;

    public StreakDailyLogController(StreakDailyLogService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StreakDailyLogRequest request) {
        return ResponseEntity.ok(service.create(request));
    }
}

