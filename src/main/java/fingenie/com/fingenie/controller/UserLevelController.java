package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.UserLevelRequest;
import fingenie.com.fingenie.service.UserLevelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-levels")
public class UserLevelController {

    private final UserLevelService service;

    public UserLevelController(UserLevelService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserLevelRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> getById(@PathVariable Long accountId) {
        return ResponseEntity.ok(service.getById(accountId));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> delete(@PathVariable Long accountId) {
        service.delete(accountId);
        return ResponseEntity.ok("Deleted");
    }
}
