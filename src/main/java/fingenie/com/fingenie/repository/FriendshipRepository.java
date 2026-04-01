package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requester = :account1 AND f.addressee = :account2) OR " +
           "(f.requester = :account2 AND f.addressee = :account1)")
    Optional<Friendship> findFriendshipBetweenAccounts(
            @Param("account1") Account account1,
            @Param("account2") Account account2
    );
    
    List<Friendship> findByRequester(Account requester);
    List<Friendship> findByAddressee(Account addressee);
    
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requester = :account OR f.addressee = :account) AND " +
           "f.status = :status")
    List<Friendship> findByAccountAndStatus(
            @Param("account") Account account,
            @Param("status") Friendship.FriendshipStatus status
    );

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
           "((f.requester.id = :accountId1 AND f.addressee.id = :accountId2) OR " +
           "(f.requester.id = :accountId2 AND f.addressee.id = :accountId1)) AND " +
           "f.status = 'ACCEPTED'")
    boolean areFriends(@Param("accountId1") Long accountId1, @Param("accountId2") Long accountId2);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
           "((f.requester.id = :accountId1 AND f.addressee.id = :accountId2) OR " +
           "(f.requester.id = :accountId2 AND f.addressee.id = :accountId1)) AND " +
           "f.status = 'PENDING'")
    boolean hasPendingRequest(@Param("accountId1") Long accountId1, @Param("accountId2") Long accountId2);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
           "(f.requester.id = :accountId OR f.addressee.id = :accountId) AND f.status = 'ACCEPTED'")
    long countFriends(@Param("accountId") Long accountId);

    @Query("SELECT CASE WHEN f.requester.id = :accountId THEN f.addressee.id ELSE f.requester.id END " +
           "FROM Friendship f WHERE (f.requester.id = :accountId OR f.addressee.id = :accountId) AND f.status = 'ACCEPTED'")
    List<Long> findFriendIds(@Param("accountId") Long accountId);
}
