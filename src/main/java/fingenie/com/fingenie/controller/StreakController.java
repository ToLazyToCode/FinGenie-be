package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.StreakRequest;
import fingenie.com.fingenie.service.StreakService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streaks")
public class StreakController {

    private final StreakService service;

    public StreakController(StreakService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StreakRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok("Deleted");
    }
}

