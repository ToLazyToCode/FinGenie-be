package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.SavingContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Repository
public interface SavingContributionRepository extends JpaRepository<SavingContribution, Long> {

    Page<SavingContribution> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    @Query("""
           SELECT COALESCE(SUM(sc.amount), 0)
           FROM SavingContribution sc
           WHERE sc.targetType = :targetType
             AND sc.targetId = :targetId
             AND sc.accountId = :accountId
           """)
    BigDecimal sumAmountByTargetTypeAndTargetIdAndAccountId(
            @Param("targetType") SavingContribution.TargetType targetType,
            @Param("targetId") Long targetId,
            @Param("accountId") Long accountId
    );

    @Query("""
           SELECT COUNT(sc)
           FROM SavingContribution sc
           WHERE sc.targetType = :targetType
             AND sc.targetId = :targetId
             AND sc.accountId = :accountId
             AND sc.createdAt >= :fromInclusive
             AND sc.createdAt < :toExclusive
           """)
    long countByTargetTypeAndTargetIdAndAccountIdAndCreatedAtRange(
            @Param("targetType") SavingContribution.TargetType targetType,
            @Param("targetId") Long targetId,
            @Param("accountId") Long accountId,
            @Param("fromInclusive") Timestamp fromInclusive,
            @Param("toExclusive") Timestamp toExclusive
    );

    @Query("""
           SELECT COUNT(DISTINCT sc.accountId)
           FROM SavingContribution sc
           WHERE sc.targetType = :targetType
             AND sc.targetId = :targetId
             AND sc.createdAt >= :fromInclusive
             AND sc.createdAt < :toExclusive
           """)
    long countDistinctAccountByTargetTypeAndTargetIdAndCreatedAtRange(
            @Param("targetType") SavingContribution.TargetType targetType,
            @Param("targetId") Long targetId,
            @Param("fromInclusive") Timestamp fromInclusive,
            @Param("toExclusive") Timestamp toExclusive
    );

    @Query("""
           SELECT COALESCE(SUM(sc.amount), 0)
           FROM SavingContribution sc
           WHERE sc.accountId = :accountId
           """)
    BigDecimal sumAmountByAccountId(@Param("accountId") Long accountId);
}
