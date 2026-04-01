package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.XPLogRequest;
import fingenie.com.fingenie.service.XPLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/xp-logs")
public class XPLogController {

    private final XPLogService service;

    public XPLogController(XPLogService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody XPLogRequest request) {
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
