package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.Transaction;
import fingenie.com.fingenie.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

/**
 * Transaction repository with OSIV-safe query methods.
 * Uses @EntityGraph for eager fetching of lazy associations where needed.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccount_IdAndTransactionDateBetween(Long accountId, Date startDate, Date endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId")
    long countTotalByAccountId(@Param("accountId") Long accountId);

    @Query("""
           SELECT COUNT(t)
           FROM Transaction t
           WHERE t.account.id = :accountId
             AND t.category.categoryType = :categoryType
           """)
    long countByAccountIdAndCategoryType(
            @Param("accountId") Long accountId,
            @Param("categoryType") Category.CategoryType categoryType
    );
    
    // ========== Basic Queries with EntityGraph for OSIV Safety ==========
    
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithRelations(@Param("id") Long id);
    
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    List<Transaction> findByAccount(Account account);

    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    Page<Transaction> findByAccount(Account account, Pageable pageable);
    
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    List<Transaction> findByAccountId(Long accountId);
    
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    List<Transaction> findByWallet(Wallet wallet);
    
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    List<Transaction> findByWalletId(Long walletId);
    
    List<Transaction> findByCategory(Category category);
    
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    List<Transaction> findByAccountAndTransactionDateBetween(
            Account account,
            Date startDate,
            Date endDate
    );
    
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    List<Transaction> findByWalletAndTransactionDateBetween(
            Wallet wallet,
            Date startDate,
            Date endDate
    );

    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    @Query("SELECT t FROM Transaction t WHERE t.account = :account ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<Transaction> findByAccountOrderByDateDesc(@Param("account") Account account);

    // Analytics queries - with EntityGraph for OSIV safety
    @EntityGraph(attributePaths = {"account", "wallet", "category"})
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate ASC")
    List<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.amount > 0")
    BigDecimal sumIncomeByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT SUM(ABS(t.amount)) FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.amount < 0")
    BigDecimal sumExpenseByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Long countByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT t.category.id, t.category.categoryName, SUM(ABS(t.amount)), COUNT(t) " +
           "FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.amount < 0 " +
           "GROUP BY t.category.id, t.category.categoryName ORDER BY SUM(ABS(t.amount)) DESC")
    List<Object[]> sumExpenseByCategory(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT t.transactionDate, SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), " +
           "SUM(CASE WHEN t.amount < 0 THEN ABS(t.amount) ELSE 0 END) " +
           "FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.transactionDate ORDER BY t.transactionDate")
    List<Object[]> getDailyTotals(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT t.description, SUM(ABS(t.amount)), COUNT(t), t.category.categoryName " +
           "FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.amount < 0 " +
           "GROUP BY t.description, t.category.categoryName ORDER BY SUM(ABS(t.amount)) DESC")
    List<Object[]> getTopMerchants(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    // Budget calculation queries
    @Query("SELECT COALESCE(SUM(ABS(t.amount)), 0) FROM Transaction t " +
           "WHERE t.account.id = :accountId " +
           "AND t.category.id = :categoryId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND t.amount < 0")
    BigDecimal sumExpensesByAccountAndCategoryBetween(
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT COALESCE(SUM(ABS(t.amount)), 0) FROM Transaction t " +
           "WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND t.amount < 0")
    BigDecimal sumExpensesByAccountBetween(
            @Param("accountId") Long accountId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("""
        SELECT DISTINCT t.account.id
        FROM Transaction t
        WHERE t.transactionDate > :date
    """)
    List<Long> findDistinctAccountIdsWithTransactionsAfter(@Param("date") Date date);

    // ── Admin dashboard queries ──────────────────────────────────────────────

    /** Sum of all positive-amount (income) transactions across all accounts. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.amount > 0")
    BigDecimal sumAllIncome();

    // ── Admin user-detail queries ────────────────────────────────────────────

    /**
     * Paginated transactions for a single account (most-recent first).
     * Eagerly fetches wallet and category to avoid N+1.
     */
    @EntityGraph(attributePaths = {"wallet", "category"})
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId "
         + "ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByAccountIdPaged(
            @Param("accountId") Long accountId,
            Pageable pageable);

    /** Sum of income (amount > 0) for a user, all time. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t "
         + "WHERE t.account.id = :accountId AND t.amount > 0")
    BigDecimal sumTotalIncomeByAccountId(@Param("accountId") Long accountId);

    /** Sum of expenses (|amount| where amount < 0) for a user, all time. */
    @Query("SELECT COALESCE(SUM(ABS(t.amount)), 0) FROM Transaction t "
         + "WHERE t.account.id = :accountId AND t.amount < 0")
    BigDecimal sumTotalExpenseByAccountId(@Param("accountId") Long accountId);
}
