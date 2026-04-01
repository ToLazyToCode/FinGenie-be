package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.EntitlementSnapshotResponse;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/entitlements")
@RequiredArgsConstructor
@Tag(name = "Entitlements", description = "Current account entitlement snapshot")
public class EntitlementController {

    private final EntitlementService entitlementService;

    @GetMapping("/me")
    @Operation(summary = "Get current user entitlement snapshot")
    public ResponseEntity<EntitlementSnapshotResponse> getCurrentEntitlements() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(entitlementService.getSnapshot(accountId));
    }
}
