package fingenie.com.fingenie.ai.repository;

import fingenie.com.fingenie.ai.entity.AIConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AIConversationRepository extends JpaRepository<AIConversationMessage, Long> {
    List<AIConversationMessage> findTop20ByUserIdOrderByTimestampDesc(Long userId);
    
    @Modifying
    @Query("DELETE FROM AIConversationMessage WHERE userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
