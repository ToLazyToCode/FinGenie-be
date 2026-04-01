package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.FriendStreakRequest;
import fingenie.com.fingenie.service.FriendStreakService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friend-streaks")
public class FriendStreakController {

    private final FriendStreakService service;

    public FriendStreakController(FriendStreakService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FriendStreakRequest request) {
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
