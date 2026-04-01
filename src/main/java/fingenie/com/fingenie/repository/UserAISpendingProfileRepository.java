package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.UserAISpendingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAISpendingProfileRepository extends JpaRepository<UserAISpendingProfile, Long> {
    Optional<UserAISpendingProfile> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
