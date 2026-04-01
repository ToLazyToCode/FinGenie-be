package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.dto.CompleteProfileResponse;
import fingenie.com.fingenie.dto.UserSettingsRequest;
import fingenie.com.fingenie.dto.UserSettingsResponse;
import fingenie.com.fingenie.service.CompleteProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile and settings APIs")
public class ProfileController {

    private final CompleteProfileService profileService;

    @GetMapping("/complete")
    @Operation(summary = "Get complete user profile with all stats")
    public ResponseEntity<CompleteProfileResponse> getCompleteProfile() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(profileService.getCompleteProfile(accountId));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get user settings")
    public ResponseEntity<UserSettingsResponse> getSettings() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(profileService.getSettings(accountId));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update user settings")
    public ResponseEntity<UserSettingsResponse> updateSettings(
            @Valid @RequestBody UserSettingsRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(profileService.updateSettings(accountId, request));
    }
}
