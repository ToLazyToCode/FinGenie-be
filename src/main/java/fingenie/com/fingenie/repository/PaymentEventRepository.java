package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    Optional<PaymentEvent> findByEventHash(String eventHash);
}
