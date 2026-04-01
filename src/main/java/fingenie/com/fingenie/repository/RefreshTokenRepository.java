package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Refresh Token operations with rotation support
 * 
 * SECURITY FEATURES:
 * - Lookup by token hash (not plain text)
 * - Family-based revocation for theft detection
 * - Device-specific session management
 * - Automatic cleanup of expired tokens
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find token by its hash
     * Primary lookup method - NEVER store/lookup plain tokens
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find token by hash for a specific account
     */
    Optional<RefreshToken> findByTokenHashAndAccountId(String tokenHash, Long accountId);

    /**
     * Find valid (not revoked, not expired) token by hash
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash " +
           "AND r.revoked = false AND r.expiresAt > :now")
    Optional<RefreshToken> findValidByTokenHash(String tokenHash, Instant now);

    /**
     * Find all active tokens for a device
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.account.id = :accountId " +
           "AND r.deviceId = :deviceId AND r.revoked = false AND r.expiresAt > :now")
    List<RefreshToken> findActiveByAccountAndDevice(Long accountId, String deviceId, Instant now);

    /**
     * Find all active tokens for an account (all devices)
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.account.id = :accountId " +
           "AND r.revoked = false AND r.expiresAt > :now ORDER BY r.createdAt DESC")
    List<RefreshToken> findActiveByAccount(Long accountId, Instant now);

    /**
     * Find all tokens in a family (for cascade revocation)
     */
    List<RefreshToken> findByTokenFamily(String tokenFamily);

    /**
     * Find active tokens in a family
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.tokenFamily = :tokenFamily " +
           "AND r.revoked = false")
    List<RefreshToken> findActiveByFamily(String tokenFamily);

    /**
     * Revoke all tokens for an account
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedReason = :reason, " +
           "r.revokedAt = :now WHERE r.account.id = :accountId AND r.revoked = false")
    int revokeAllByAccountId(Long accountId, String reason, Instant now);

    /**
     * Revoke all tokens for an account on a specific device
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedReason = :reason, " +
           "r.revokedAt = :now WHERE r.account.id = :accountId " +
           "AND r.deviceId = :deviceId AND r.revoked = false")
    int revokeAllByAccountAndDevice(Long accountId, String deviceId, String reason, Instant now);

    /**
     * Revoke all tokens in a family (for theft detection)
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedReason = :reason, " +
           "r.revokedAt = :now WHERE r.tokenFamily = :tokenFamily AND r.revoked = false")
    int revokeAllByFamily(String tokenFamily, String reason, Instant now);

    /**
     * Count active sessions for an account
     */
    @Query("SELECT COUNT(DISTINCT r.deviceId) FROM RefreshToken r " +
           "WHERE r.account.id = :accountId AND r.revoked = false " +
           "AND r.expiresAt > :now AND r.deviceId IS NOT NULL")
    int countActiveDevices(Long accountId, Instant now);

    /**
     * Delete expired tokens (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    int deleteExpiredTokens(Instant cutoff);

    /**
     * Delete old revoked tokens (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true AND r.revokedAt < :cutoff")
    int deleteOldRevokedTokens(Instant cutoff);

    /**
     * Check if a rotated token has been reused (theft detection)
     */
    @Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash AND r.rotated = true")
    Optional<RefreshToken> findRotatedToken(String tokenHash);

    // Legacy method for backward compatibility
    @Deprecated
    default Optional<RefreshToken> findByToken(String token) {
        return findByTokenHash(token);
    }

    // Legacy method - now takes reason parameter
    @Deprecated
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.account.id = :accountId")
    void revokeAllByAccountId(Long accountId);
}

