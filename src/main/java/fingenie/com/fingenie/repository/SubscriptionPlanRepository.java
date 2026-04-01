package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByPlanCodeAndIsActiveTrue(String planCode);

    Optional<SubscriptionPlan> findByPlanCode(String planCode);

    List<SubscriptionPlan> findByIsActiveTrueOrderBySortOrderAscIdAsc();
}
