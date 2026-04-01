package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AIChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    
    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
    
    @Query("SELECT cm FROM AIChatMessage cm WHERE cm.conversation.id = :conversationId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("conversationId") Long conversationId, Pageable pageable);
    
    long countByConversationId(Long conversationId);
    
    @Query("SELECT COALESCE(SUM(cm.tokenCount), 0) FROM AIChatMessage cm WHERE cm.conversation.id = :conversationId")
    int getTotalTokens(@Param("conversationId") Long conversationId);

    @Query("""
           SELECT COUNT(cm)
           FROM AIChatMessage cm
           WHERE cm.conversation.accountId = :accountId
             AND UPPER(cm.sender) = 'USER'
             AND cm.createdAt >= :fromInclusive
             AND cm.createdAt < :toExclusive
           """)
    long countUserMessagesByAccountIdAndCreatedAtRange(
            @Param("accountId") Long accountId,
            @Param("fromInclusive") Timestamp fromInclusive,
            @Param("toExclusive") Timestamp toExclusive
    );
    
    void deleteByConversationId(Long conversationId);
}
