package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.UserProvider;
import fingenie.com.fingenie.entity.UserProvider.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User Provider operations
 * 
 * Supports multi-provider authentication with:
 * - Provider lookup by type and user ID
 * - Account linking queries
 * - Provider management
 */
@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {

    /**
     * Find provider by type and external user ID
     * Used during OAuth login to find existing linked accounts
     */
    Optional<UserProvider> findByProviderAndProviderUserId(ProviderType provider, String providerUserId);

    /**
     * Find all providers linked to an account
     */
    List<UserProvider> findByAccountId(Long accountId);

    /**
     * Find active providers for an account
     */
    List<UserProvider> findByAccountIdAndActiveTrue(Long accountId);

    /**
     * Find provider by account and type
     */
    Optional<UserProvider> findByAccountIdAndProvider(Long accountId, ProviderType provider);

    /**
     * Check if provider is already linked to any account
     */
    boolean existsByProviderAndProviderUserId(ProviderType provider, String providerUserId);

    /**
     * Check if account has a specific provider linked
     */
    boolean existsByAccountIdAndProvider(Long accountId, ProviderType provider);

    /**
     * Find by email (for account linking)
     */
    List<UserProvider> findByEmail(String email);

    /**
     * Find active provider by email and type
     */
    Optional<UserProvider> findByEmailAndProviderAndActiveTrue(String email, ProviderType provider);

    /**
     * Count active providers for account (for unlink validation)
     */
    @Query("SELECT COUNT(p) FROM UserProvider p WHERE p.account.id = :accountId AND p.active = true")
    int countActiveProvidersByAccountId(Long accountId);

    /**
     * Deactivate provider (soft delete)
     */
    @Modifying
    @Query("UPDATE UserProvider p SET p.active = false WHERE p.id = :providerId")
    void deactivateProvider(Long providerId);

    /**
     * Deactivate all providers for account (for account deletion)
     */
    @Modifying
    @Query("UPDATE UserProvider p SET p.active = false WHERE p.account.id = :accountId")
    void deactivateAllForAccount(Long accountId);
}
