package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ai_conversation_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversationMessage extends BaseEntity {

    private Long userId;

    private String role;

    @Column(length = 2000)
    private String message;

    private Instant timestamp;
}
