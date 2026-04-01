package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE :accountId MEMBER OF c.participantIds AND c.isActive = true ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByParticipantId(@Param("accountId") Long accountId);

    @Query("SELECT c FROM Conversation c WHERE :user1 MEMBER OF c.participantIds AND :user2 MEMBER OF c.participantIds AND c.isActive = true")
    Optional<Conversation> findByParticipants(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE :accountId MEMBER OF c.participantIds AND c.isActive = true")
    long countByParticipantId(@Param("accountId") Long accountId);
}
