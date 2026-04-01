package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.CustomException;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.RefreshToken;
import fingenie.com.fingenie.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token Service with Bank-Grade Security
 * 
 * FEATURES:
 * - Secure token generation with cryptographic randomness
 * - Token hashing (SHA-256) - never store plain tokens
 * - Token rotation on each refresh
 * - Theft detection via rotation reuse
 * - Device binding for multi-device support
 * - Family-based cascade revocation
 * - Automatic cleanup of expired tokens
 * 
 * SECURITY MODEL:
 * 1. Each refresh creates a new token (rotation)
 * 2. Old token is marked as "rotated" (not revoked immediately)
 * 3. If rotated token is reused = THEFT DETECTED
 * 4. On theft: entire token family is revoked
 * 5. Device binding prevents cross-device token abuse
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpirationMs; // Default 7 days

    @Value("${security.max-active-devices:5}")
    private int maxActiveDevices;

    /**
     * Generate a new refresh token for an account
     * 
     * @param account The account to create token for
     * @param deviceId Device identifier (can be null)
     * @param deviceInfo Device description (user agent, etc.)
     * @param ipAddress Client IP address
     * @return The plain text token (to return to client)
     */
    @Transactional
    public String generateToken(Account account, String deviceId, String deviceInfo, String ipAddress) {
        // Check device limit
        if (deviceId != null) {
            int activeDevices = refreshTokenRepository.countActiveDevices(account.getId(), Instant.now());
            if (activeDevices >= maxActiveDevices) {
                log.warn("Max devices reached for account {}, revoking oldest session", account.getId());
                revokeOldestSession(account.getId());
            }
        }

        // Generate cryptographically secure token
        String plainToken = generateSecureToken();
        String tokenHash = hashToken(plainToken);
        String tokenFamily = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .account(account)
                .deviceId(deviceId)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS))
                .tokenFamily(tokenFamily)
                .generation(0)
                .build();

        refreshTokenRepository.save(refreshToken);
        
        log.debug("Generated new refresh token for account {} on device {}", account.getId(), deviceId);
        return plainToken;
    }

    /**
     * Rotate refresh token - creates new token, marks old as rotated
     * 
     * SECURITY: This is the core of token rotation:
     * 1. Validates current token
     * 2. Marks current token as rotated
     * 3. Creates new token in same family with incremented generation
     * 4. Returns new plain token
     * 
     * @param currentPlainToken The current refresh token
     * @param deviceId Device making the request
     * @return New plain text token, or throws if invalid/theft detected
     */
    @Transactional
    public String rotateToken(String currentPlainToken, String deviceId) {
        String tokenHash = hashToken(currentPlainToken);
        
        // Check for token reuse (theft detection)
        Optional<RefreshToken> rotatedOpt = refreshTokenRepository.findRotatedToken(tokenHash);
        if (rotatedOpt.isPresent()) {
            // THEFT DETECTED - token already rotated but being reused
            RefreshToken rotatedToken = rotatedOpt.get();
            log.error("SECURITY ALERT: Token theft detected! Token family: {}, Account: {}", 
                    rotatedToken.getTokenFamily(), rotatedToken.getAccount().getId());
            
            // Revoke entire token family
            revokeTokenFamily(rotatedToken.getTokenFamily(), "TOKEN_THEFT_DETECTED");
            
            // Log security event (implement SecurityEventService as needed)
            logSecurityEvent(rotatedToken.getAccount().getId(), "TOKEN_THEFT_DETECTED", 
                    "Rotated token reused - all sessions revoked");
            
            throw new CustomException(HttpStatus.UNAUTHORIZED, "TOKEN_THEFT_DETECTED",
                    "Security alert: Your session has been compromised. Please login again.");
        }

        // Find valid token
        RefreshToken currentToken = refreshTokenRepository.findValidByTokenHash(tokenHash, Instant.now())
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid or expired"));

        // Verify device binding if enabled
        if (currentToken.getDeviceId() != null && deviceId != null && 
                !currentToken.getDeviceId().equals(deviceId)) {
            log.warn("Device mismatch for token. Expected: {}, Got: {}", 
                    currentToken.getDeviceId(), deviceId);
            throw new CustomException(HttpStatus.UNAUTHORIZED, "DEVICE_MISMATCH",
                    "Token cannot be used from this device");
        }

        // Generate new token
        String newPlainToken = generateSecureToken();
        String newTokenHash = hashToken(newPlainToken);

        RefreshToken newToken = RefreshToken.builder()
                .tokenHash(newTokenHash)
                .account(currentToken.getAccount())
                .deviceId(deviceId != null ? deviceId : currentToken.getDeviceId())
                .deviceInfo(currentToken.getDeviceInfo())
                .ipAddress(currentToken.getIpAddress())
                .expiresAt(Instant.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS))
                .tokenFamily(currentToken.getTokenFamily())
                .generation(currentToken.getGeneration() + 1)
                .build();

        RefreshToken savedNewToken = refreshTokenRepository.save(newToken);

        // Mark old token as rotated (not revoked - for theft detection)
        currentToken.markRotated(savedNewToken.getId());
        refreshTokenRepository.save(currentToken);

        log.debug("Rotated refresh token for account {} (generation {})", 
                currentToken.getAccount().getId(), newToken.getGeneration());
        
        return newPlainToken;
    }

    /**
     * Validate token and get associated account
     */
    @Transactional(readOnly = true)
    public Optional<Account> validateAndGetAccount(String plainToken) {
        String tokenHash = hashToken(plainToken);
        
        return refreshTokenRepository.findValidByTokenHash(tokenHash, Instant.now())
                .map(RefreshToken::getAccount);
    }

    /**
     * Revoke all tokens for an account (logout all devices)
     */
    @Transactional
    public void revokeAllForAccount(Long accountId, String reason) {
        int revoked = refreshTokenRepository.revokeAllByAccountId(accountId, reason, Instant.now());
        log.info("Revoked {} tokens for account {} (reason: {})", revoked, accountId, reason);
    }

    /**
     * Revoke all tokens for a specific device
     */
    @Transactional
    public void revokeAllForDevice(Long accountId, String deviceId, String reason) {
        int revoked = refreshTokenRepository.revokeAllByAccountAndDevice(
                accountId, deviceId, reason, Instant.now());
        log.info("Revoked {} tokens for account {} device {} (reason: {})", 
                revoked, accountId, deviceId, reason);
    }

    /**
     * Revoke entire token family (for theft detection)
     */
    @Transactional
    public void revokeTokenFamily(String tokenFamily, String reason) {
        int revoked = refreshTokenRepository.revokeAllByFamily(tokenFamily, reason, Instant.now());
        log.warn("Revoked {} tokens in family {} (reason: {})", revoked, tokenFamily, reason);
    }

    /**
     * Get active sessions for an account
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveSessions(Long accountId) {
        return refreshTokenRepository.findActiveByAccount(accountId, Instant.now());
    }

    /**
     * Revoke a specific token
     */
    @Transactional
    public void revokeToken(String plainToken, String reason) {
        String tokenHash = hashToken(plainToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke(reason);
            refreshTokenRepository.save(token);
            log.debug("Revoked token for account {}", token.getAccount().getId());
        });
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Generate cryptographically secure random token
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[64]; // 512 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hash token using SHA-256
     */
    public String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Revoke the oldest session when max devices reached
     */
    private void revokeOldestSession(Long accountId) {
        List<RefreshToken> sessions = refreshTokenRepository.findActiveByAccount(accountId, Instant.now());
        if (!sessions.isEmpty()) {
            RefreshToken oldest = sessions.get(sessions.size() - 1); // List is ordered by createdAt DESC
            oldest.revoke("MAX_DEVICES_EXCEEDED");
            refreshTokenRepository.save(oldest);
        }
    }

    /**
     * Log security event (extend as needed)
     */
    private void logSecurityEvent(Long accountId, String eventType, String details) {
        log.error("SECURITY EVENT - Account: {}, Type: {}, Details: {}", accountId, eventType, details);
        // TODO: Implement SecurityEventService for persistent logging
        // TODO: Send notification to user about security event
    }

    // ============================================
    // Scheduled Cleanup Jobs
    // ============================================

    /**
     * Clean up expired tokens (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        // Delete tokens expired more than 7 days ago
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Cleaned up {} expired tokens", deleted);
    }

    /**
     * Clean up old revoked tokens (runs weekly)
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void cleanupRevokedTokens() {
        // Delete tokens revoked more than 30 days ago
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = refreshTokenRepository.deleteOldRevokedTokens(cutoff);
        log.info("Cleaned up {} old revoked tokens", deleted);
    }
}
