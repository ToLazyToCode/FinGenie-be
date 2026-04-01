package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Refund;
import fingenie.com.fingenie.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /** Paginated refunds filtered by status (e.g. PENDING). */
    Page<Refund> findByStatus(RefundStatus status, Pageable pageable);

    /** All refunds for a specific payment order. */
    List<Refund> findByPaymentOrderId(Long paymentOrderId);

    /** Count of refunds in a given status – used for refund-rate calculation. */
    long countByStatus(RefundStatus status);
}
