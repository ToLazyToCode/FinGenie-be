package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.UserSubscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserSubscription s WHERE s.accountId = :accountId")
    Optional<UserSubscription> findByAccountIdForUpdate(@Param("accountId") Long accountId);
}
