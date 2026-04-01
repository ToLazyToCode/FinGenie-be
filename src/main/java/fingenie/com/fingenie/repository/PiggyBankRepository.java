package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.Wallet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {
    Optional<PiggyBank> findByWallet(Wallet wallet);
    Optional<PiggyBank> findByWalletId(Long walletId);
    boolean existsByWalletId(Long walletId);
    boolean existsByIdAndWalletAccountIdAndIsSharedFalse(Long piggyId, Long accountId);
    long countByWalletAccountId(Long accountId);

    @Query("""
            SELECT pb
            FROM PiggyBank pb
            WHERE pb.wallet.account.id = :accountId
               OR pb.id IN (
                    SELECT member.piggyBank.id
                    FROM PiggyBankMember member
                    WHERE member.account.id = :accountId
               )
            ORDER BY pb.createdAt DESC
            """)
    List<PiggyBank> findAccessibleByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
