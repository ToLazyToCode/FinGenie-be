package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.AIConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIConversationEntityRepository extends JpaRepository<AIConversation, Long> {
    
    List<AIConversation> findByAccountIdOrderByUpdatedAtDesc(Long accountId);
    
    List<AIConversation> findByAccountIdAndIsActiveTrueOrderByUpdatedAtDesc(Long accountId, Pageable pageable);
    
    @Query("SELECT c FROM AIConversation c WHERE c.accountId = :accountId AND c.isActive = true ORDER BY c.updatedAt DESC")
    List<AIConversation> findActiveConversations(@Param("accountId") Long accountId, Pageable pageable);
    
    Optional<AIConversation> findFirstByAccountIdAndIsActiveTrueOrderByUpdatedAtDesc(Long accountId);
    
    @Query("SELECT c FROM AIConversation c WHERE c.accountId = :accountId AND c.id = :conversationId")
    Optional<AIConversation> findByIdAndAccountId(@Param("conversationId") Long conversationId, 
                                                    @Param("accountId") Long accountId);
    
    long countByAccountId(Long accountId);
    
    void deleteByAccountId(Long accountId);
}
