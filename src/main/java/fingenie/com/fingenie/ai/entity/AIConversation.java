package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Conversation entity for storing chat sessions with the AI.
 */
@Entity
@Table(name = "ai_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversation extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "title")
    private String title;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    @Column(name = "total_tokens")
    @Builder.Default
    private Integer totalTokens = 0;
}
