package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.dto.GoogleAuthRequest;
import fingenie.com.fingenie.dto.GoogleUserInfo;
import fingenie.com.fingenie.entity.AuthAccount;
import fingenie.com.fingenie.entity.RefreshToken;
import fingenie.com.fingenie.entity.UserGamification;
import fingenie.com.fingenie.entity.UserProvider;
import fingenie.com.fingenie.entity.UserProvider.ProviderType;
import fingenie.com.fingenie.entity.PetProfile;
import fingenie.com.fingenie.entity.Pet;
import fingenie.com.fingenie.repository.RefreshTokenRepository;
import fingenie.com.fingenie.repository.UserGamificationRepository;
import fingenie.com.fingenie.repository.UserProviderRepository;
import fingenie.com.fingenie.repository.PetProfileRepository;
import fingenie.com.fingenie.repository.PetRepository;
import fingenie.com.fingenie.repository.WalletRepository;
import fingenie.com.fingenie.entity.Wallet;
import fingenie.com.fingenie.common.CustomException;
import fingenie.com.fingenie.constraints.AppMessage;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Account.AuthProvider;
import fingenie.com.fingenie.entity.UserProfile;
import fingenie.com.fingenie.service.GoogleOAuth2Service;
import fingenie.com.fingenie.service.otp.OtpPurpose;
import fingenie.com.fingenie.service.otp.OtpService;
import fingenie.com.fingenie.service.otp.OtpSession;
import fingenie.com.fingenie.service.otp.ResetTokenStore;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.UserProfileRepository;
import fingenie.com.fingenie.utils.ParseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserProviderRepository userProviderRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final PetProfileRepository petProfileRepository;
    private final PetRepository petRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final ResetTokenStore resetTokenStore;
    private final GoogleOAuth2Service googleOAuth2Service;
    private final RefreshTokenService refreshTokenService;
    private final AccountLinkingService accountLinkingService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Step 1: Register - create OTP session and send OTP. Account not created yet.
     */
    public RegisterOtpResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", AppMessage.USER_ALREADY_EXISTS);
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "PASSWORDS_DO_NOT_MATCH", AppMessage.PASSWORDS_DO_NOT_MATCH);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        String sessionId = otpService.createRegisterSession(request, passwordHash);
        return new RegisterOtpResponse(sessionId, request.getEmail(), 300, "OTP sent to your email. Please verify to complete registration.");
    }

    /**
     * Step 2: Verify email OTP - create account and return tokens.
     */
    @Transactional
    public AuthResponse verifyEmailOtp(String sessionId, String otp) {
        OtpSession session = otpService.validateOtp(sessionId, otp, OtpPurpose.REGISTER_EMAIL_VERIFY);

        if (accountRepository.existsByEmail(session.getEmail())) {
            throw new CustomException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", AppMessage.USER_ALREADY_EXISTS);
        }

        // Create Account
        Account account = new Account();
        account.setEmail(session.getEmail());
        account.setPassword(session.getPasswordHash());
        account.setRole(Account.Role.USER);
        accountRepository.save(account);

        // Create User Profile
        UserProfile userProfile = new UserProfile();
        userProfile.setAccount(account);
        userProfile.setFullName(session.getFullName());
        userProfile.setDateOfBirth(ParseUtil.parseStringToDate(session.getDateOfBirth()));
        userProfileRepository.save(userProfile);

        // Create Gamification Profile
        UserGamification gamification = UserGamification.builder()
                .accountId(account.getId())
                .xp(0)
                .level(1)
                .build();
        userGamificationRepository.save(gamification);

        // Create Pet Profile (pet state)
        PetProfile petProfile = PetProfile.builder()
                .accountId(account.getId())
                .mood(50)
                .energy(50)
                .hunger(50)
                .happiness(50)
                .build();
        petProfileRepository.save(petProfile);

        // Create default Pet
        Pet pet = new Pet();
        pet.setAccountId(account.getId());
        pet.setPetName("Fini");
        pet.setPetType("cat");
        pet.setLevel(1);
        pet.setExperiencePoints(0);
        pet.setMood("happy");
        pet.setPersonality("friendly");
        petRepository.save(pet);

        // Create default wallets
        createDefaultWalletsForAccount(account);

        return issueTokens(account);
    }

    /**
     * Resend OTP for registration flow.
     */
    public RegisterOtpResponse resendRegisterOtp(String sessionId) {
        OtpSession session = otpService.getSessionForResend(sessionId);
        if (session.getPurpose() != OtpPurpose.REGISTER_EMAIL_VERIFY) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_SESSION", "Invalid session for registration OTP.");
        }
        String newSessionId = otpService.resendOtp(sessionId, OtpPurpose.REGISTER_EMAIL_VERIFY);
        return new RegisterOtpResponse(newSessionId, session.getEmail(), 300, "New OTP sent to your email.");
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load account entity
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return issueTokens(account);
    }

    /**
     * Login with Google OAuth2 - Enhanced with secure account linking.
     * 
     * Flow:
     * 1. Verify Google ID token
     * 2. Check if Google provider is already linked → login
     * 3. Check if email exists with password → return ACCOUNT_LINK_REQUIRED
     * 4. Create new account if no conflict
     */
    @Transactional
    public GoogleAuthResponse loginWithGoogle(GoogleAuthRequest request) {
        // Verify Google ID token
        GoogleUserInfo googleUser = googleOAuth2Service.verifyIdToken(request.getIdToken(), request.getPlatform());
        
        log.info("Google login attempt for email: {}", googleUser.getEmail());
        
        // Step 1: Check if Google provider is already linked
        Optional<UserProvider> existingProvider = userProviderRepository
                .findByProviderAndProviderUserId(ProviderType.GOOGLE, googleUser.getId());
        
        if (existingProvider.isPresent() && existingProvider.get().isActive()) {
            // Existing Google link - login directly
            Account account = existingProvider.get().getAccount();
            log.info("Existing Google provider found for account: {}", account.getId());
            return issueGoogleAuthTokens(account, request.getDeviceId());
        }
        
        // Step 2: Check if email already exists (potential linking scenario)
        Optional<Account> existingEmailAccount = accountRepository.findByEmail(googleUser.getEmail());
        
        if (existingEmailAccount.isPresent()) {
            Account account = existingEmailAccount.get();
            
            // Check if account has password (requires linking verification)
            boolean hasPassword = account.getPassword() != null && !account.getPassword().isEmpty();
            
            if (hasPassword) {
                // SECURITY: Require password verification for linking
                log.info("Account exists with password, requiring linking for: {}", googleUser.getEmail());
                
                String linkToken = accountLinkingService.generateLinkToken(
                        googleUser.getEmail(), 
                        request.getIdToken()
                );
                
                List<String> existingProviders = accountLinkingService.getExistingProviderNames(googleUser.getEmail());
                existingProviders.add(0, "EMAIL"); // Email/password auth
                
                return GoogleAuthResponse.linkRequired(
                        googleUser.getEmail(),
                        linkToken,
                        true,
                        existingProviders
                );
            } else {
                // No password - auto-link Google provider (OAuth-only account)
                log.info("Auto-linking Google to OAuth-only account: {}", googleUser.getEmail());
                createProviderLink(account, googleUser);
                return issueGoogleAuthTokens(account, request.getDeviceId());
            }
        }
        
        // Step 3: Create new account
        log.info("Creating new account for Google user: {}", googleUser.getEmail());
        Account newAccount = createGoogleAccount(googleUser);
        return issueGoogleAuthTokens(newAccount, request.getDeviceId());
    }
    
    /**
     * Complete Google account linking after password verification
     */
    @Transactional
    public GoogleAuthResponse completeGoogleLinking(String linkToken, String password, String deviceId) {
        accountLinkingService.linkGoogleWithToken(linkToken, password, deviceId);
        
        // Link was successful, get account and issue tokens
        // The link token contains the email, we need to get it
        Account account = accountRepository.findByEmail(
                // We need to get email from the validated session
                // For now, we'll re-fetch after successful linking
                // This will be handled by the AccountLinkingService returning the account
                accountLinkingService.getExistingProviderNames(linkToken).isEmpty() 
                        ? "" : "" // This needs proper implementation
        ).orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found after linking."));
        
        return issueGoogleAuthTokens(account, deviceId);
    }
    
    /**
     * Create provider link for existing account
     */
    private void createProviderLink(Account account, GoogleUserInfo googleUser) {
        UserProvider provider = UserProvider.builder()
                .account(account)
                .provider(ProviderType.GOOGLE)
                .providerUserId(googleUser.getId())
                .email(googleUser.getEmail())
                .emailVerified(googleUser.isEmailVerified())
                .displayName(googleUser.getName())
                .pictureUrl(googleUser.getPictureUrl())
                .build();
        
        userProviderRepository.save(provider);
        
        // Also update legacy fields for backward compatibility
        account.setProvider(AuthProvider.GOOGLE);
        account.setProviderId(googleUser.getId());
        account.setEmailVerified(googleUser.isEmailVerified());
        accountRepository.save(account);
    }

    /**
     * Creates a new account for a Google OAuth user.
     */
    private Account createGoogleAccount(GoogleUserInfo googleUser) {
        // Create Account
        Account account = new Account();
        account.setEmail(googleUser.getEmail());
        account.setName(googleUser.getName());
        account.setPassword(null); // OAuth users don't have passwords initially
        account.setRole(Account.Role.USER);
        account.setProvider(AuthProvider.GOOGLE);
        account.setProviderId(googleUser.getId());
        account.setEmailVerified(googleUser.isEmailVerified());
        accountRepository.save(account);
        
        // Create UserProvider link (new multi-provider system)
        UserProvider provider = UserProvider.builder()
                .account(account)
                .provider(ProviderType.GOOGLE)
                .providerUserId(googleUser.getId())
                .email(googleUser.getEmail())
                .emailVerified(googleUser.isEmailVerified())
                .displayName(googleUser.getName())
                .pictureUrl(googleUser.getPictureUrl())
                .build();
        userProviderRepository.save(provider);
        
        // Create User Profile
        UserProfile userProfile = new UserProfile();
        userProfile.setAccount(account);
        userProfile.setFullName(googleUser.getName());
        userProfile.setAvatarUrl(googleUser.getPictureUrl());
        userProfileRepository.save(userProfile);
        
        // Create Gamification Profile
        UserGamification gamification = UserGamification.builder()
                .accountId(account.getId())
                .xp(0)
                .level(1)
                .build();
        userGamificationRepository.save(gamification);
        
        // Create Pet Profile (pet state)
        PetProfile petProfile = PetProfile.builder()
                .accountId(account.getId())
                .mood(50)
                .energy(50)
                .hunger(50)
                .happiness(50)
                .build();
        petProfileRepository.save(petProfile);
        
        // Create default Pet
        Pet pet = new Pet();
        pet.setAccountId(account.getId());
        pet.setPetName("Fini");
        pet.setPetType("cat");
        pet.setLevel(1);
        pet.setExperiencePoints(0);
        pet.setMood("happy");
        pet.setPersonality("friendly");
        petRepository.save(pet);

        // Create default wallets
        createDefaultWalletsForAccount(account);
        
        log.info("Successfully created new Google account with ID: {}", account.getId());
        return account;
    }

    /**
     * Refresh token with secure rotation - BANK-GRADE SECURITY
     * 
     * Security features:
     * - Token rotation on every refresh
     * - Theft detection (using rotated token triggers cascade revocation)
     * - Device binding validation
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String oldRefreshToken = request.getRefreshToken();
        String deviceId = request.getDeviceId();
        
        // Get account from the old token first
        Account account = refreshTokenService.validateAndGetAccount(oldRefreshToken)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or expired"));
        
        // Rotate token (this also validates and handles theft detection)
        String newRefreshToken = refreshTokenService.rotateToken(oldRefreshToken, deviceId);
        
        // Generate new access token
        AuthAccount authAccount = new AuthAccount(account);
        String accessToken = jwtService.generateToken(authAccount, account.getId());

        return new AuthResponse(accessToken, newRefreshToken, jwtExpiration, 
                account.getId(), account.getEmail(), account.getName());
    }
    
    /**
     * Legacy refresh token method for backward compatibility
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        // Legacy method - call new method with null deviceId
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);
        request.setDeviceId(null);
        return refreshToken(request);
    }

    @Transactional
    public void logout() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthAccount auth)) {
            throw new RuntimeException("Not authenticated");
        }

        refreshTokenRepository.revokeAllByAccountId(
                auth.getAccount().getId()
        );
    }

    /**
     * Check if the current session/token is still valid.
     * Used for heartbeat and cross-device logout detection.
     */
    public SessionStatusResponse getSessionStatus() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthAccount auth)) {
            return SessionStatusResponse.invalid(SessionStatusResponse.SessionInvalidReason.SESSION_EXPIRED);
        }

        // If we reach here, token is valid and user is authenticated
        return SessionStatusResponse.valid();
    }

    /**
     * Forgot Password - Step 1: Request OTP.
     */
    public ForgotPasswordOtpResponse forgotPasswordRequestOtp(String email) {
        // Don't reveal if email exists - return same message for security
        if (!accountRepository.existsByEmail(email)) {
            return new ForgotPasswordOtpResponse(null, "If an account exists with this email, an OTP has been sent.");
        }
        String sessionId = otpService.createForgotPasswordSession(email);
        return new ForgotPasswordOtpResponse(sessionId, "If an account exists with this email, an OTP has been sent.");
    }

    /**
     * Forgot Password - Step 2: Verify OTP and get reset token.
     */
    public ForgotPasswordVerifyResponse forgotPasswordVerifyOtp(String sessionId, String otp) {
        OtpSession session = otpService.validateOtp(sessionId, otp, OtpPurpose.FORGOT_PASSWORD);
        String resetToken = resetTokenStore.createToken(session.getEmail());
        return new ForgotPasswordVerifyResponse(resetToken, "OTP verified. You can now reset your password.");
    }

    /**
     * Forgot Password - Step 3: Reset password with token.
     */
    @Transactional
    public void forgotPasswordReset(String resetToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "PASSWORDS_DO_NOT_MATCH", AppMessage.PASSWORDS_DO_NOT_MATCH);
        }

        String email = resetTokenStore.getEmailAndDelete(resetToken)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Invalid or expired reset token. Please request a new one."));

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found."));

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    /**
     * Resend OTP for forgot password flow.
     */
    public ForgotPasswordOtpResponse resendForgotPasswordOtp(String sessionId) {
        OtpSession session = otpService.getSessionForResend(sessionId);
        if (session.getPurpose() != OtpPurpose.FORGOT_PASSWORD) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_SESSION", "Invalid session for forgot password OTP.");
        }
        String newSessionId = otpService.resendOtp(sessionId, OtpPurpose.FORGOT_PASSWORD);
        return new ForgotPasswordOtpResponse(newSessionId, "New OTP sent to your email.");
    }

    private AuthResponse issueTokens(Account account) {
        return issueTokensWithDevice(account, null);
    }
    
    /**
     * Issue tokens with device binding for enhanced security
     */
    private AuthResponse issueTokensWithDevice(Account account, String deviceId) {
        AuthAccount authAccount = new AuthAccount(account);

        String accessToken = jwtService.generateToken(authAccount, account.getId());
        
        // Use RefreshTokenService for secure token generation
        String refreshToken = refreshTokenService.generateToken(account, deviceId, null, null);

        return new AuthResponse(accessToken, refreshToken, jwtExpiration, 
                account.getId(), account.getEmail(), account.getName());
    }
    
    /**
     * Issue tokens for Google auth response with device binding
     */
    private GoogleAuthResponse issueGoogleAuthTokens(Account account, String deviceId) {
        AuthAccount authAccount = new AuthAccount(account);

        String accessToken = jwtService.generateToken(authAccount, account.getId());
        
        // Use RefreshTokenService for secure token generation
        String refreshToken = refreshTokenService.generateToken(account, deviceId, null, null);

        return GoogleAuthResponse.success(
                accessToken, 
                refreshToken, 
                jwtExpiration, 
                account.getId(), 
                account.getEmail(), 
                account.getName()
        );
    }

    /**
     * Creates default wallets for a newly registered account.
     * - Regular wallet: "Ví" (Vietnamese) as default
     * - Piggy Bank: "Heo Đất" (Vietnamese)
     */
    private void createDefaultWalletsForAccount(Account account) {
        // Create default Regular wallet
        Wallet regularWallet = Wallet.builder()
                .account(account)
                .walletName("Ví")
                .balance(java.math.BigDecimal.ZERO)
                .isDefault(true)
                .build();
        walletRepository.save(regularWallet);

        // Create Piggy Bank
        Wallet piggyWallet = Wallet.builder()
                .account(account)
                .walletName("Heo Đất")
                .balance(java.math.BigDecimal.ZERO)
                .isDefault(false)
                .build();
        walletRepository.save(piggyWallet);

        log.debug("Created default wallets for account ID: {}", account.getId());
    }
}
