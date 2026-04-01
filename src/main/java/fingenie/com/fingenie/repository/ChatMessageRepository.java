package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.senderId != :accountId AND m.isRead = false")
    List<ChatMessage> findUnreadMessages(@Param("conversationId") Long conversationId, @Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP WHERE m.conversationId = :conversationId AND m.senderId != :accountId AND m.isRead = false")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("accountId") Long accountId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.senderId != :accountId AND m.isRead = false")
    long countUnread(@Param("conversationId") Long conversationId, @Param("accountId") Long accountId);

    @Query("SELECT m.conversationId, COUNT(m) FROM ChatMessage m WHERE m.conversationId IN :conversationIds AND m.senderId != :accountId AND m.isRead = false GROUP BY m.conversationId")
    List<Object[]> countUnreadByConversations(@Param("conversationIds") List<Long> conversationIds, @Param("accountId") Long accountId);
}
