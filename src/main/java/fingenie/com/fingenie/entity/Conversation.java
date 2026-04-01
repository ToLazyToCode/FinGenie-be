package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Chat conversation between friends.
 */
@Entity
@Table(name = "conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

    @ElementCollection
    @CollectionTable(name = "conversation_participants", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "account_id")
    @Builder.Default
    private Set<Long> participantIds = new HashSet<>();

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview")
    private String lastMessagePreview;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void addParticipant(Long accountId) {
        this.participantIds.add(accountId);
    }

    public boolean hasParticipant(Long accountId) {
        return this.participantIds.contains(accountId);
    }
}
