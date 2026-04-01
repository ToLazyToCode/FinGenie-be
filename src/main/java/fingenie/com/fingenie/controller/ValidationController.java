package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.ValidationRequest;
import fingenie.com.fingenie.dto.ValidationResponse;
import fingenie.com.fingenie.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Validation Controller
 * 
 * Secure validation endpoints for async field validation.
 * 
 * SECURITY PRINCIPLES:
 * 1. NEVER reveal whether an account exists (enumeration protection)
 * 2. Use generic error messages
 * 3. Rate-limited by ValidationRateLimitFilter
 * 4. Return consistent response times (prevent timing attacks)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validate")
@RequiredArgsConstructor
@Tag(name = "Validation", description = "Field validation endpoints")
public class ValidationController {

    private final ValidationService validationService;

    /**
     * Validate email format and availability
     * 
     * SECURITY: Returns generic "invalid" for both format issues and existing emails
     * to prevent account enumeration
     */
    @Operation(
            summary = "Validate email",
            description = "Validates email format. Does NOT reveal if email is already registered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/email")
    public ResponseEntity<ValidationResponse> validateEmail(
            @Valid @RequestBody ValidationRequest.Email request
    ) {
        ValidationResponse response = validationService.validateEmail(request.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Validate username format and availability
     */
    @Operation(
            summary = "Validate username",
            description = "Validates username format and checks availability"
    )
    @PostMapping("/username")
    public ResponseEntity<ValidationResponse> validateUsername(
            @Valid @RequestBody ValidationRequest.Username request
    ) {
        ValidationResponse response = validationService.validateUsername(request.username());
        return ResponseEntity.ok(response);
    }

    /**
     * Validate phone number format
     */
    @Operation(
            summary = "Validate phone number",
            description = "Validates phone number format for Vietnamese numbers"
    )
    @PostMapping("/phone")
    public ResponseEntity<ValidationResponse> validatePhone(
            @Valid @RequestBody ValidationRequest.Phone request
    ) {
        ValidationResponse response = validationService.validatePhone(request.phone());
        return ResponseEntity.ok(response);
    }

    /**
     * Validate password strength
     * 
     * Returns detailed strength analysis for UX but enforces minimum requirements
     */
    @Operation(
            summary = "Validate password strength",
            description = "Analyzes password strength and returns detailed feedback"
    )
    @PostMapping("/password")
    public ResponseEntity<ValidationResponse.PasswordStrength> validatePassword(
            @Valid @RequestBody ValidationRequest.Password request
    ) {
        ValidationResponse.PasswordStrength response = validationService.validatePassword(request.password());
        return ResponseEntity.ok(response);
    }

    /**
     * Validate wallet name (for uniqueness within user's wallets)
     */
    @Operation(
            summary = "Validate wallet name",
            description = "Validates wallet name format and checks uniqueness for current user"
    )
    @PostMapping("/wallet-name")
    public ResponseEntity<ValidationResponse> validateWalletName(
            @Valid @RequestBody ValidationRequest.WalletName request
    ) {
        ValidationResponse response = validationService.validateWalletName(
                request.walletName(),
                request.excludeWalletId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Validate transaction amount
     */
    @Operation(
            summary = "Validate transaction amount",
            description = "Validates amount is positive and within limits"
    )
    @PostMapping("/amount")
    public ResponseEntity<ValidationResponse> validateAmount(
            @Valid @RequestBody ValidationRequest.Amount request
    ) {
        ValidationResponse response = validationService.validateAmount(request.amount());
        return ResponseEntity.ok(response);
    }
}
