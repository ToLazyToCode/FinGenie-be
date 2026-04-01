package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByAccount(Account account);
    Optional<UserProfile> findByAccountId(Long accountId);
    boolean existsByAccount(Account account);
}
