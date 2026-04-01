package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByAccountId(Long accountId);
}
