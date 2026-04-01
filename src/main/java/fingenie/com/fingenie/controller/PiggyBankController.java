package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.PiggyBankRequest;
import fingenie.com.fingenie.dto.PiggyBankResponse;
import fingenie.com.fingenie.service.PiggyBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/piggy-banks")
@RequiredArgsConstructor
public class PiggyBankController {

    private final PiggyBankService piggyBankService;

    @PostMapping
    public PiggyBankResponse create(@Valid @RequestBody PiggyBankRequest request) {
        return piggyBankService.create(request);
    }

    @GetMapping
    public List<PiggyBankResponse> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        return piggyBankService.getAll(page, size);
    }

    @GetMapping("/{piggyId}")
    public PiggyBankResponse getById(@PathVariable Long piggyId) {
        return piggyBankService.getById(piggyId);
    }

    @PutMapping("/{piggyId}")
    public PiggyBankResponse update(
            @PathVariable Long piggyId,
            @Valid @RequestBody PiggyBankRequest request
    ) {
        return piggyBankService.update(piggyId, request);
    }

    @DeleteMapping("/{piggyId}")
    public ResponseEntity<?> delete(@PathVariable Long piggyId) {
        piggyBankService.delete(piggyId);
        return ResponseEntity.ok(Map.of("message", "Piggy bank deleted successfully"));
    }
}
