package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.AutoAllocatePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutoAllocatePolicyRepository extends JpaRepository<AutoAllocatePolicy, Long> {
    Optional<AutoAllocatePolicy> findByAccountId(Long accountId);
}
