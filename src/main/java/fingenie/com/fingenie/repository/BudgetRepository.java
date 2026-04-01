package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Budget;
import fingenie.com.fingenie.entity.Budget.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByAccountIdAndIsActiveTrue(Long accountId);
    
    List<Budget> findByAccountIdAndPeriodTypeAndIsActiveTrue(Long accountId, PeriodType periodType);
    
    @Query("SELECT b FROM Budget b WHERE b.account.id = :accountId AND b.category IS NULL AND b.isActive = true")
    Optional<Budget> findTotalBudget(@Param("accountId") Long accountId);
    
    @Query("SELECT b FROM Budget b WHERE b.account.id = :accountId AND b.category.id = :categoryId " +
           "AND b.periodType = :periodType AND b.isActive = true")
    Optional<Budget> findByCategoryAndPeriod(
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("periodType") PeriodType periodType);
    
    @Query("SELECT b FROM Budget b WHERE b.account.id = :accountId AND b.category.id = :categoryId AND b.isActive = true")
    List<Budget> findByCategoryId(@Param("accountId") Long accountId, @Param("categoryId") Long categoryId);
    
    @Query("SELECT b FROM Budget b WHERE b.isActive = true AND b.notifyOnWarning = true")
    List<Budget> findAllActiveWithWarningEnabled();
    
    @Query("SELECT b FROM Budget b WHERE b.isActive = true AND b.notifyOnExceed = true")
    List<Budget> findAllActiveWithExceedEnabled();
    
    boolean existsByAccountIdAndCategoryIdAndPeriodTypeAndIsActiveTrue(
            Long accountId, Long categoryId, PeriodType periodType);
}
