package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.UserLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {

    Optional<UserLevel> findByAccountId(Long accountId);

    void deleteByAccountId(Long accountId);
    
    // Leaderboard queries
    List<UserLevel> findAllByOrderByLifetimeXpDesc(Pageable pageable);
    
    List<UserLevel> findAllByOrderByCurrentLevelDescCurrentXpDesc(Pageable pageable);
    
    @Query("SELECT COUNT(ul) + 1 FROM UserLevel ul WHERE ul.lifetimeXp > " +
           "(SELECT ul2.lifetimeXp FROM UserLevel ul2 WHERE ul2.accountId = :accountId)")
    Long findRankByAccountId(@Param("accountId") Long accountId);
    
    @Query("SELECT ul FROM UserLevel ul WHERE ul.accountId IN :accountIds ORDER BY ul.lifetimeXp DESC")
    List<UserLevel> findByAccountIdsOrderByLifetimeXp(@Param("accountIds") List<Long> accountIds);
    
    long count();
}
