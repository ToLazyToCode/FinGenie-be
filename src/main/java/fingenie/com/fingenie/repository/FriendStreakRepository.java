package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.FriendStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendStreakRepository extends JpaRepository<FriendStreak, Long> {
    
    @Query("SELECT COUNT(fs) FROM FriendStreak fs JOIN Friendship f ON fs.friendshipId = f.id " +
           "WHERE (f.requester.id = :accountId OR f.addressee.id = :accountId) AND fs.isActive = true")
    int countByAccountIdAndIsActiveTrue(@Param("accountId") Long accountId);
}
