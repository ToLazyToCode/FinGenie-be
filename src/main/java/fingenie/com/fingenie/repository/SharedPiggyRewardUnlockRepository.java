package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.SharedPiggyRewardUnlock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedPiggyRewardUnlockRepository extends JpaRepository<SharedPiggyRewardUnlock, Long> {

    Optional<SharedPiggyRewardUnlock> findByPiggyBankIdAndMilestoneKey(Long piggyId, String milestoneKey);

    List<SharedPiggyRewardUnlock> findByPiggyBankIdOrderByUnlockedAtDesc(Long piggyId);

    List<SharedPiggyRewardUnlock> findByPiggyBankIdOrderByUnlockedAtDesc(Long piggyId, Pageable pageable);
}
