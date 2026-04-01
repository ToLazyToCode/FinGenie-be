package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.dto.GoogleAuthRequest;
import fingenie.com.fingenie.entity.AuthAccount;
import fingenie.com.fingenie.entity.UserProvider.ProviderType;
import fingenie.com.fingenie.service.AuthenticationService;
import fingenie.com.fingenie.service.AccountLinkingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication, registration, account linking, and password management APIs")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AccountLinkingService accountLinkingService;

    /* ----- Register Flow (with OTP) ----- */

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Initiates user registration by sending a 6-digit OTP to the provided email. " +
                     "The OTP expires in 5 minutes. Rate limited: max 5 requests per email per 10 minutes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent successfully",
            content = @Content(schema = @Schema(implementation = RegisterOtpResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or email already registered"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded - too many OTP requests")
    })
    public RegisterOtpResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/verify-email-otp")
    @Operation(
        summary = "Verify email OTP and complete registration",
        description = "Verifies the 6-digit OTP sent during registration. On success, returns JWT tokens. " +
                     "Max 5 attempts per session. After 5 failed attempts, the session is blacklisted."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful, tokens returned",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
        @ApiResponse(responseCode = "429", description = "Too many failed attempts - session blacklisted")
    })
    public AuthResponse verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return authenticationService.verifyEmailOtp(request.getSessionId(), request.getOtp());
    }

    @PostMapping("/resend-otp")
    @Operation(
        summary = "Resend registration OTP",
        description = "Resends OTP for ongoing registration session. 60-second cooldown between resends. " +
                     "Previous OTP is invalidated when a new one is sent."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New OTP sent successfully",
            content = @Content(schema = @Schema(implementation = RegisterOtpResponse.class))),
        @ApiResponse(responseCode = "400", description = "Session not found or expired"),
        @ApiResponse(responseCode = "429", description = "Cooldown active - wait before resending")
    })
    public RegisterOtpResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authenticationService.resendRegisterOtp(request.getSessionId());
    }

    /* ----- Forgot Password Flow ----- */

    @PostMapping("/forgot-password/request-otp")
    @Operation(
        summary = "Request password reset OTP",
        description = "Sends a 6-digit OTP to the registered email for password reset. " +
                     "Rate limited: max 5 requests per email per 10 minutes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent if email exists",
            content = @Content(schema = @Schema(implementation = ForgotPasswordOtpResponse.class))),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ForgotPasswordOtpResponse forgotPasswordRequestOtp(@Valid @RequestBody ForgotPasswordRequestOtpRequest request) {
        return authenticationService.forgotPasswordRequestOtp(request.getEmail());
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(
        summary = "Verify password reset OTP",
        description = "Verifies the OTP for password reset. Returns a single-use reset token valid for 15 minutes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP verified, reset token returned",
            content = @Content(schema = @Schema(implementation = ForgotPasswordVerifyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
        @ApiResponse(responseCode = "429", description = "Too many failed attempts")
    })
    public ForgotPasswordVerifyResponse forgotPasswordVerifyOtp(@Valid @RequestBody ForgotPasswordVerifyOtpRequest request) {
        return authenticationService.forgotPasswordVerifyOtp(request.getSessionId(), request.getOtp());
    }

    @PostMapping("/forgot-password/reset")
    @Operation(
        summary = "Reset password with token",
        description = "Resets the user's password using the reset token from OTP verification. " +
                     "Token is single-use and expires after 15 minutes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid reset token or password mismatch")
    })
    public ResponseEntity<Map<String, String>> forgotPasswordReset(@Valid @RequestBody ForgotPasswordResetRequest request) {
        authenticationService.forgotPasswordReset(
                request.getResetToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/forgot-password/resend-otp")
    @Operation(
        summary = "Resend password reset OTP",
        description = "Resends OTP for ongoing password reset session. 60-second cooldown between resends."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New OTP sent successfully",
            content = @Content(schema = @Schema(implementation = ForgotPasswordOtpResponse.class))),
        @ApiResponse(responseCode = "400", description = "Session not found or expired"),
        @ApiResponse(responseCode = "429", description = "Cooldown active")
    })
    public ForgotPasswordOtpResponse resendForgotPasswordOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authenticationService.resendForgotPasswordOtp(request.getSessionId());
    }

    /* ----- Login / Refresh / Logout ----- */

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates user with email and password. Returns JWT access and refresh tokens."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/google")
    @Operation(
        summary = "Google OAuth login/registration",
        description = "Authenticates user with Google ID token. Creates new account if user doesn't exist. " +
                     "If user already has a local account with the same email and password, returns ACCOUNT_LINK_REQUIRED " +
                     "with a link token. Use /auth/link/complete to complete linking."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login/registration successful or linking required",
            content = @Content(schema = @Schema(implementation = GoogleAuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired Google ID token"),
        @ApiResponse(responseCode = "500", description = "Google OAuth service not configured")
    })
    public GoogleAuthResponse loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        return authenticationService.loginWithGoogle(request);
    }
    
    @PostMapping("/link/complete")
    @Operation(
        summary = "Complete Google account linking",
        description = "Completes Google account linking after ACCOUNT_LINK_REQUIRED response. " +
                     "Requires the link token and account password for verification."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Linking completed, tokens returned",
            content = @Content(schema = @Schema(implementation = GoogleAuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired link token"),
        @ApiResponse(responseCode = "401", description = "Invalid password")
    })
    public GoogleAuthResponse completeLinking(@Valid @RequestBody CompleteLinkingRequest request) {
        return authenticationService.completeGoogleLinking(
                request.getLinkToken(),
                request.getPassword(),
                request.getDeviceId()
        );
    }
    
    @PostMapping("/link/google")
    @Operation(
        summary = "Link Google account to existing account",
        description = "Links a Google account to the currently logged-in user's account. " +
                     "Requires password verification for security."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Google account linked successfully",
            content = @Content(schema = @Schema(implementation = AccountLinkingResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid password or not logged in"),
        @ApiResponse(responseCode = "409", description = "Google account already linked to another user")
    })
    public AccountLinkingResponse linkGoogle(
            @AuthenticationPrincipal AuthAccount authAccount,
            @Valid @RequestBody LinkGoogleAccountRequest request
    ) {
        return accountLinkingService.linkGoogleDirect(authAccount.getAccount().getId(), request);
    }
    
    @DeleteMapping("/unlink/{provider}")
    @Operation(
        summary = "Unlink authentication provider",
        description = "Unlinks an authentication provider (GOOGLE, APPLE, etc.) from the account. " +
                     "Cannot unlink the last authentication method. Password required if set."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Provider unlinked successfully",
            content = @Content(schema = @Schema(implementation = AccountLinkingResponse.class))),
        @ApiResponse(responseCode = "400", description = "Cannot unlink last authentication method"),
        @ApiResponse(responseCode = "401", description = "Invalid password"),
        @ApiResponse(responseCode = "404", description = "Provider not linked")
    })
    public AccountLinkingResponse unlinkProvider(
            @AuthenticationPrincipal AuthAccount authAccount,
            @PathVariable String provider,
            @Valid @RequestBody UnlinkProviderRequest request
    ) {
        ProviderType providerType = ProviderType.valueOf(provider.toUpperCase());
        return accountLinkingService.unlinkProvider(
                authAccount.getAccount().getId(),
                providerType,
                request.getPassword()
        );
    }
    
    @GetMapping("/providers")
    @Operation(
        summary = "Get linked providers",
        description = "Returns list of all authentication providers linked to the current account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of linked providers"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public List<AccountLinkingResponse.ProviderInfo> getLinkedProviders(
            @AuthenticationPrincipal AuthAccount authAccount
    ) {
        return accountLinkingService.getLinkedProviders(authAccount.getAccount().getId());
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token with secure rotation",
        description = "Issues new access and refresh tokens using a valid refresh token. " +
                     "SECURITY: Token is rotated on every refresh. Using a rotated token triggers " +
                     "theft detection and revokes all tokens for the device family."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New tokens issued",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token"),
        @ApiResponse(responseCode = "403", description = "Theft detected - all sessions revoked")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refreshToken(request);
    }
    
    @PostMapping("/refresh/header")
    @Operation(
        summary = "Refresh access token (legacy header-based)",
        description = "Legacy endpoint for refreshing tokens via Authorization header. " +
                     "Prefer using POST /auth/refresh with JSON body for device binding."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New tokens issued",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public AuthResponse refreshLegacy(
        @Parameter(description = "Bearer refresh token", required = true, example = "Bearer eyJhbGci...")
        @RequestHeader("Authorization") String refreshToken
    ) {
        return authenticationService.refreshToken(refreshToken.replace("Bearer ", ""));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "User logout",
        description = "Invalidates the current session. Requires a valid access token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> logout() {
        authenticationService.logout();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/session-status")
    @Operation(
        summary = "Check session status",
        description = "Checks if the current session is still valid. Used for heartbeat and cross-device logout detection."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session status returned",
            content = @Content(schema = @Schema(implementation = SessionStatusResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public SessionStatusResponse sessionStatus() {
        return authenticationService.getSessionStatus();
    }
}
