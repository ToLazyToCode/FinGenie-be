package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByAccount(Account account);
    List<Wallet> findByAccountId(Long accountId);
    long countByAccountId(Long accountId);
    Optional<Wallet> findByAccountAndIsDefaultTrue(Account account);
    Optional<Wallet> findByAccountIdAndIsDefaultTrue(Long accountId);

    // For validation: check wallet name uniqueness within user's wallets
    boolean existsByAccountIdAndWalletNameIgnoreCase(Long accountId, String walletName);
    boolean existsByAccountIdAndWalletNameIgnoreCaseAndIdNot(Long accountId, String walletName, Long excludeWalletId);

    // ── Admin user-detail query ──────────────────────────────────────────────

    /** Sum of all wallet balances owned by a user. */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w WHERE w.account.id = :accountId")
    BigDecimal sumBalanceByAccountId(@Param("accountId") Long accountId);
}
