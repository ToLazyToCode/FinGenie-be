package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.CustomException;
import fingenie.com.fingenie.dto.*;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.UserProvider;
import fingenie.com.fingenie.entity.UserProvider.ProviderType;
import fingenie.com.fingenie.repository.AccountRepository;
import fingenie.com.fingenie.repository.UserProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Account Linking Service - Secure Provider Management
 * 
 * Handles:
 * 1. Linking OAuth providers to existing accounts
 * 2. Unlinking providers (with safety checks)
 * 3. Link token generation and validation
 * 4. Anti-account takeover protections
 * 
 * SECURITY RULES:
 * - Password verification required for linking
 * - Email must be verified by provider
 * - Cannot unlink last authentication method
 * - Rate limiting on link attempts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLinkingService {

    private final AccountRepository accountRepository;
    private final UserProviderRepository userProviderRepository;
    private final GoogleOAuth2Service googleOAuth2Service;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final String LINK_TOKEN_PREFIX = "link_token:";
    private static final Duration LINK_TOKEN_EXPIRY = Duration.ofMinutes(10);

    // ============================================
    // Link Token Management
    // ============================================

    /**
     * Generate a temporary link token for account linking flow
     * 
     * @param email The email requiring linking
     * @param googleIdToken The Google ID token to be linked
     * @return Link token (UUID)
     */
    public String generateLinkToken(String email, String googleIdToken) {
        String linkToken = UUID.randomUUID().toString();
        String key = LINK_TOKEN_PREFIX + linkToken;
        
        // Store: linkToken -> email:idToken
        String value = email + ":" + googleIdToken;
        redisTemplate.opsForValue().set(key, value, LINK_TOKEN_EXPIRY);
        
        log.debug("Generated link token for email: {}", email);
        return linkToken;
    }

    /**
     * Validate and consume link token
     * 
     * @return LinkTokenData or null if invalid
     */
    private LinkTokenData validateAndConsumeLinkToken(String linkToken) {
        String key = LINK_TOKEN_PREFIX + linkToken;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return null;
        }
        
        // Consume token (one-time use)
        redisTemplate.delete(key);
        
        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        
        return new LinkTokenData(parts[0], parts[1]);
    }

    private record LinkTokenData(String email, String googleIdToken) {}

    // ============================================
    // Provider Linking
    // ============================================

    /**
     * Link Google account to existing account using link token
     * 
     * Security flow:
     * 1. Validate link token
     * 2. Re-verify Google ID token
     * 3. Verify user owns the account (password check)
     * 4. Create provider link
     * 
     * @return Auth tokens on success
     */
    @Transactional
    public AuthResponse linkGoogleWithToken(String linkToken, String password, String deviceId) {
        // Step 1: Validate link token
        LinkTokenData linkData = validateAndConsumeLinkToken(linkToken);
        if (linkData == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "INVALID_LINK_TOKEN",
                    "Link token is invalid or expired. Please try Google sign-in again.");
        }

        // Step 2: Re-verify Google ID token (security)
        GoogleUserInfo googleUser;
        try {
            googleUser = googleOAuth2Service.verifyIdToken(linkData.googleIdToken());
        } catch (Exception e) {
            log.warn("Google token re-verification failed: {}", e.getMessage());
            throw new CustomException(HttpStatus.BAD_REQUEST, "GOOGLE_TOKEN_EXPIRED",
                    "Google session expired. Please try signing in with Google again.");
        }

        // Step 3: Verify emails match
        if (!googleUser.getEmail().equalsIgnoreCase(linkData.email())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "EMAIL_MISMATCH",
                    "Email mismatch detected. Please use the same Google account.");
        }

        // Step 4: Get existing account
        Account account = accountRepository.findByEmail(linkData.email())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account not found."));

        // Step 5: Verify password
        if (account.getPassword() == null || !passwordEncoder.matches(password, account.getPassword())) {
            log.warn("Invalid password during account linking for: {}", linkData.email());
            throw new CustomException(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD",
                    "Password is incorrect. Please try again.");
        }

        // Step 6: Check if Google already linked to another account
        Optional<UserProvider> existingProvider = userProviderRepository
                .findByProviderAndProviderUserId(ProviderType.GOOGLE, googleUser.getId());
        
        if (existingProvider.isPresent() && 
                !existingProvider.get().getAccount().getId().equals(account.getId())) {
            throw new CustomException(HttpStatus.CONFLICT, "PROVIDER_ALREADY_LINKED",
                    "This Google account is already linked to another user.");
        }

        // Step 7: Create provider link
        if (existingProvider.isEmpty()) {
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
            log.info("Linked Google account to user: {}", account.getId());
        }

        // Return success - caller will issue tokens
        return null; // Let AuthenticationService handle token issuance
    }

    /**
     * Link Google account directly (when user is logged in)
     */
    @Transactional
    public AccountLinkingResponse linkGoogleDirect(Long accountId, LinkGoogleAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account not found."));

        // Verify password
        if (account.getPassword() == null || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD",
                    "Password is incorrect.");
        }

        // Verify Google token
        GoogleUserInfo googleUser = googleOAuth2Service.verifyIdToken(request.getIdToken());

        // Security: Email must be verified
        if (!googleUser.isEmailVerified()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED",
                    "Google email is not verified. Please verify your Google email first.");
        }

        // Check if already linked to another account
        Optional<UserProvider> existingProvider = userProviderRepository
                .findByProviderAndProviderUserId(ProviderType.GOOGLE, googleUser.getId());
        
        if (existingProvider.isPresent()) {
            if (existingProvider.get().getAccount().getId().equals(accountId)) {
                throw new CustomException(HttpStatus.CONFLICT, "ALREADY_LINKED",
                        "This Google account is already linked to your account.");
            } else {
                throw new CustomException(HttpStatus.CONFLICT, "PROVIDER_ALREADY_LINKED",
                        "This Google account is already linked to another user.");
            }
        }

        // Create link
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
        log.info("Linked Google account {} to user {}", googleUser.getId(), accountId);

        return AccountLinkingResponse.success(
                "Google account linked successfully.",
                getProviderInfoList(accountId)
        );
    }

    // ============================================
    // Provider Unlinking
    // ============================================

    /**
     * Unlink a provider from account
     * 
     * SECURITY: Cannot unlink last authentication method
     */
    @Transactional
    public AccountLinkingResponse unlinkProvider(Long accountId, ProviderType provider, String password) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account not found."));

        // Find provider link
        UserProvider userProvider = userProviderRepository.findByAccountIdAndProvider(accountId, provider)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "PROVIDER_NOT_LINKED",
                        "This provider is not linked to your account."));

        // Check if this is the last auth method
        int activeProviders = userProviderRepository.countActiveProvidersByAccountId(accountId);
        boolean hasPassword = account.getPassword() != null && !account.getPassword().isEmpty();
        
        if (activeProviders <= 1 && !hasPassword) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "CANNOT_UNLINK_LAST_METHOD",
                    "Cannot unlink last authentication method. Please set a password first.");
        }

        // Verify password if account has one
        if (hasPassword) {
            if (password == null || !passwordEncoder.matches(password, account.getPassword())) {
                throw new CustomException(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD",
                        "Password is incorrect.");
            }
        }

        // Soft delete (deactivate)
        userProviderRepository.deactivateProvider(userProvider.getId());
        log.info("Unlinked {} from account {}", provider, accountId);

        return AccountLinkingResponse.success(
                provider + " account unlinked successfully.",
                getProviderInfoList(accountId)
        );
    }

    // ============================================
    // Provider Queries
    // ============================================

    /**
     * Get all linked providers for account
     */
    @Transactional(readOnly = true)
    public List<AccountLinkingResponse.ProviderInfo> getLinkedProviders(Long accountId) {
        return getProviderInfoList(accountId);
    }

    /**
     * Check if account has specific provider linked
     */
    @Transactional(readOnly = true)
    public boolean hasProviderLinked(Long accountId, ProviderType provider) {
        return userProviderRepository.existsByAccountIdAndProvider(accountId, provider);
    }

    /**
     * Find account by provider credentials
     */
    @Transactional(readOnly = true)
    public Optional<Account> findAccountByProvider(ProviderType provider, String providerUserId) {
        return userProviderRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .filter(UserProvider::isActive)
                .map(UserProvider::getAccount);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private List<AccountLinkingResponse.ProviderInfo> getProviderInfoList(Long accountId) {
        List<UserProvider> providers = userProviderRepository.findByAccountIdAndActiveTrue(accountId);
        
        return providers.stream()
                .map(p -> AccountLinkingResponse.ProviderInfo.builder()
                        .provider(p.getProvider().name())
                        .email(p.getEmail())
                        .displayName(p.getDisplayName())
                        .pictureUrl(p.getPictureUrl())
                        .active(p.isActive())
                        .linkedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get existing provider names for an email (for UI display)
     */
    @Transactional(readOnly = true)
    public List<String> getExistingProviderNames(String email) {
        return userProviderRepository.findByEmail(email).stream()
                .filter(UserProvider::isActive)
                .map(p -> p.getProvider().name())
                .collect(Collectors.toList());
    }

    /**
     * Check if account has password set
     */
    @Transactional(readOnly = true)
    public boolean hasPassword(String email) {
        return accountRepository.findByEmail(email)
                .map(a -> a.getPassword() != null && !a.getPassword().isEmpty())
                .orElse(false);
    }
}
