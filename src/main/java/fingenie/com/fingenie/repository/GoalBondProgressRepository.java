package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.GoalBondProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoalBondProgressRepository extends JpaRepository<GoalBondProgress, Long> {

    Optional<GoalBondProgress> findByPiggyBankId(Long piggyBankId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT progress FROM GoalBondProgress progress WHERE progress.piggyBank.id = :piggyBankId")
    Optional<GoalBondProgress> findByPiggyBankIdForUpdate(@Param("piggyBankId") Long piggyBankId);

    boolean existsByPiggyBankId(Long piggyBankId);
}
