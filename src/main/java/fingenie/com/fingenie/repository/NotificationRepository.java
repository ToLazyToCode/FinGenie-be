package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    List<Notification> findTop50ByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Query("SELECT n FROM Notification n WHERE n.accountId = :accountId AND n.isRead = false ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findUnreadByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.accountId = :accountId AND n.isRead = false")
    long countUnreadByAccountId(@Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.accountId = :accountId AND n.isRead = false")
    int markAllAsReadByAccountId(@Param("accountId") Long accountId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.accountId = :accountId AND n.createdAt < :cutoff")
    int deleteOldNotificationsByAccountId(@Param("accountId") Long accountId, @Param("cutoff") LocalDateTime cutoff);

    List<Notification> findByAccountIdAndTypeAndIsReadFalse(Long accountId, String type);

    @Query("SELECT n FROM Notification n WHERE n.accountId = :accountId AND n.type IN :types ORDER BY n.createdAt DESC")
    List<Notification> findByAccountIdAndTypes(@Param("accountId") Long accountId, @Param("types") List<String> types);
}
