package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.GoalBondMissionClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalBondMissionClaimRepository extends JpaRepository<GoalBondMissionClaim, Long> {

    Optional<GoalBondMissionClaim> findByPiggyBankIdAndAccountIdAndMissionIdAndMissionDay(
            Long piggyBankId,
            Long accountId,
            String missionId,
            LocalDate missionDay
    );

    List<GoalBondMissionClaim> findByPiggyBankIdAndAccountIdAndMissionDayOrderByCreatedAtDesc(
            Long piggyBankId,
            Long accountId,
            LocalDate missionDay
    );
}
