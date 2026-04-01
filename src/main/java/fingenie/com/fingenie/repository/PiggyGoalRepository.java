package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyGoalRepository extends JpaRepository<Goal, Long> {
    
    List<Goal> findByAccountId(Long accountId);
    
    List<Goal> findByAccountIdOrderByDeadlineAsc(Long accountId);
    
    Optional<Goal> findByIdAndAccountId(Long id, Long accountId);
    
    @Query("SELECT pg FROM Goal pg WHERE pg.accountId = :accountId AND pg.currentAmount >= pg.targetAmount")
    List<Goal> findCompletedGoals(@Param("accountId") Long accountId);
    
    @Query("SELECT pg FROM Goal pg WHERE pg.accountId = :accountId AND pg.currentAmount < pg.targetAmount")
    List<Goal> findActiveGoals(@Param("accountId") Long accountId);
    
    @Query("SELECT COUNT(pg) FROM Goal pg WHERE pg.accountId = :accountId AND pg.currentAmount >= pg.targetAmount")
    long countCompletedGoals(@Param("accountId") Long accountId);
    
    @Query("SELECT COALESCE(SUM(pg.currentAmount), 0) FROM Goal pg WHERE pg.accountId = :accountId")
    BigDecimal getTotalSaved(@Param("accountId") Long accountId);
    
    @Query("SELECT COALESCE(SUM(pg.targetAmount), 0) FROM Goal pg WHERE pg.accountId = :accountId")
    BigDecimal getTotalTargeted(@Param("accountId") Long accountId);
}
