package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, Long> {
    Optional<UserGamification> findByAccountId(Long accountId);
}
