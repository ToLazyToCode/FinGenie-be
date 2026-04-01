package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.PetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {
    Optional<PetProfile> findByAccountId(Long accountId);
}
