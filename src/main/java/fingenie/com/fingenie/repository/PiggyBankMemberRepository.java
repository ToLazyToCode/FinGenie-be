package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.PiggyBank;
import fingenie.com.fingenie.entity.PiggyBankMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankMemberRepository extends JpaRepository<PiggyBankMember, Long> {
    List<PiggyBankMember> findByPiggyBank(PiggyBank piggyBank);
    List<PiggyBankMember> findByPiggyBankId(Long piggyBankId);
    List<PiggyBankMember> findByAccount(Account account);
    List<PiggyBankMember> findByAccountId(Long accountId);
    Optional<PiggyBankMember> findByPiggyBankAndAccount(PiggyBank piggyBank, Account account);
    boolean existsByPiggyBankAndAccount(PiggyBank piggyBank, Account account);
    Optional<PiggyBankMember> findByPiggyBankIdAndAccountId(Long piggyBankId, Long accountId);
    boolean existsByPiggyBankIdAndAccountId(Long piggyBankId, Long accountId);
    Optional<PiggyBankMember> findByIdAndPiggyBankId(Long id, Long piggyBankId);

    @Query("SELECT COALESCE(SUM(m.shareWeight), 0) FROM PiggyBankMember m WHERE m.piggyBank.id = :piggyId")
    Long sumShareWeightByPiggyBankId(@Param("piggyId") Long piggyId);
}
