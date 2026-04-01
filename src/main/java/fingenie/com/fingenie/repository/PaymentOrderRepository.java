package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.PaymentGateway;
import fingenie.com.fingenie.entity.PaymentOrder;
import fingenie.com.fingenie.entity.PaymentOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(String orderCode);

    Optional<PaymentOrder> findByOrderCodeAndAccountId(String orderCode, Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PaymentOrder o WHERE o.orderCode = :orderCode")
    Optional<PaymentOrder> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    // ── Admin financial queries ───────────────────────────────────────────────

    /**
     * Paginated admin view of payment orders with optional filters.
     * Null parameters are treated as "no filter".
     */
    @EntityGraph(attributePaths = {"plan"})
    @Query("""
           SELECT o FROM PaymentOrder o
           WHERE (:status  IS NULL OR o.status  = :status)
             AND (:gateway IS NULL OR o.gateway = :gateway)
             AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR o.createdAt >= :dateFrom)
             AND (CAST(:dateTo   AS TIMESTAMP) IS NULL OR o.createdAt <= :dateTo)
           ORDER BY o.createdAt DESC
           """)
    Page<PaymentOrder> findForAdmin(
            @Param("status")   PaymentOrderStatus status,
            @Param("gateway")  PaymentGateway gateway,
            @Param("dateFrom") Timestamp dateFrom,
            @Param("dateTo")   Timestamp dateTo,
            Pageable pageable);

    /** Single payment order with plan eagerly loaded. */
    @EntityGraph(attributePaths = {"plan"})
    @Query("SELECT o FROM PaymentOrder o WHERE o.id = :id")
    Optional<PaymentOrder> findByIdWithPlan(@Param("id") Long id);

    // ── Summary aggregates ────────────────────────────────────────────────────

    /** Sum of order amounts for a given status since a point in time (VND). */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PaymentOrder o " +
           "WHERE o.status = :status AND o.createdAt >= :since")
    Long sumAmountByStatusSince(
            @Param("status") PaymentOrderStatus status,
            @Param("since")  Timestamp since);

    /** All-time sum of order amounts for a given status (VND). */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PaymentOrder o WHERE o.status = :status")
    Long sumAmountByStatus(@Param("status") PaymentOrderStatus status);

    /** Count of orders in a given status. */
    long countByStatus(PaymentOrderStatus status);

    /** Average amount of orders in a given status. Returns null if no rows. */
    @Query("SELECT AVG(o.amount) FROM PaymentOrder o WHERE o.status = :status")
    Double avgAmountByStatus(@Param("status") PaymentOrderStatus status);

    // ── Per-gateway aggregates ────────────────────────────────────────────────

    /** Total orders routed through a gateway. */
    long countByGateway(PaymentGateway gateway);

    /** Orders routed through a gateway that reached a specific status. */
    long countByGatewayAndStatus(PaymentGateway gateway, PaymentOrderStatus status);

    /** Sum of order amounts for a gateway + status combination (VND). */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PaymentOrder o " +
           "WHERE o.gateway = :gateway AND o.status = :status")
    Long sumAmountByGatewayAndStatus(
            @Param("gateway") PaymentGateway gateway,
            @Param("status")  PaymentOrderStatus status);

    // ── Gateway analytics with date range ────────────────────────────────────

    @Query("""
           SELECT COUNT(o) FROM PaymentOrder o
           WHERE o.gateway = :gateway
             AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR o.createdAt >= :dateFrom)
             AND (CAST(:dateTo   AS TIMESTAMP) IS NULL OR o.createdAt <= :dateTo)
           """)
    Long countByGatewayAndDateRange(
            @Param("gateway")  PaymentGateway gateway,
            @Param("dateFrom") Timestamp dateFrom,
            @Param("dateTo")   Timestamp dateTo);

    @Query("""
           SELECT COUNT(o) FROM PaymentOrder o
           WHERE o.gateway = :gateway AND o.status = :status
             AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR o.createdAt >= :dateFrom)
             AND (CAST(:dateTo   AS TIMESTAMP) IS NULL OR o.createdAt <= :dateTo)
           """)
    Long countByGatewayAndStatusAndDateRange(
            @Param("gateway")  PaymentGateway gateway,
            @Param("status")   PaymentOrderStatus status,
            @Param("dateFrom") Timestamp dateFrom,
            @Param("dateTo")   Timestamp dateTo);

    @Query("""
           SELECT COALESCE(SUM(o.amount), 0) FROM PaymentOrder o
           WHERE o.gateway = :gateway AND o.status = :status
             AND (CAST(:dateFrom AS TIMESTAMP) IS NULL OR o.createdAt >= :dateFrom)
             AND (CAST(:dateTo   AS TIMESTAMP) IS NULL OR o.createdAt <= :dateTo)
           """)
    Long sumAmountByGatewayAndStatusAndDateRange(
            @Param("gateway")  PaymentGateway gateway,
            @Param("status")   PaymentOrderStatus status,
            @Param("dateFrom") Timestamp dateFrom,
            @Param("dateTo")   Timestamp dateTo);
}
