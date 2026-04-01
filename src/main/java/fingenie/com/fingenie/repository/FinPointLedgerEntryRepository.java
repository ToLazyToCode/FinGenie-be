package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.FinPointLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinPointLedgerEntryRepository extends JpaRepository<FinPointLedgerEntry, Long> {

    Optional<FinPointLedgerEntry> findByIdempotencyKey(String idempotencyKey);

    Optional<FinPointLedgerEntry> findByAccountIdAndMissionIdAndMissionDay(Long accountId, String missionId, LocalDate missionDay);

    Page<FinPointLedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    List<FinPointLedgerEntry> findByAccountIdAndMissionDayOrderByCreatedAtDesc(Long accountId, LocalDate missionDay);

    List<FinPointLedgerEntry> findByAccountIdAndSourceTypeAndMissionDayOrderByCreatedAtDesc(
            Long accountId,
            FinPointLedgerEntry.SourceType sourceType,
            LocalDate missionDay
    );

    @Query("""
           SELECT COALESCE(SUM(entry.amount), 0)
           FROM FinPointLedgerEntry entry
           WHERE entry.accountId = :accountId
           """)
    Long sumAmountByAccountId(@Param("accountId") Long accountId);

    @Query("""
           SELECT COALESCE(SUM(entry.amount), 0)
           FROM FinPointLedgerEntry entry
           WHERE entry.accountId = :accountId
             AND entry.missionDay = :missionDay
           """)
    Long sumAmountByAccountIdAndMissionDay(
            @Param("accountId") Long accountId,
            @Param("missionDay") LocalDate missionDay
    );

    @Query("""
           SELECT COALESCE(SUM(entry.amount), 0)
           FROM FinPointLedgerEntry entry
           WHERE entry.accountId = :accountId
             AND entry.createdAt >= :fromInclusive
             AND entry.createdAt < :toExclusive
           """)
    Long sumAmountByAccountIdAndCreatedAtRange(
            @Param("accountId") Long accountId,
            @Param("fromInclusive") Timestamp fromInclusive,
            @Param("toExclusive") Timestamp toExclusive
    );
}
