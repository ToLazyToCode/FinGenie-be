package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.UserAchievementRequest;
import fingenie.com.fingenie.service.UserAchievementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-achievements")
public class UserAchievementController {

    private final UserAchievementService service;

    public UserAchievementController(UserAchievementService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserAchievementRequest request) {
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
