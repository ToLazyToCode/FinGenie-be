package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * AI Chat message entity for storing conversation history with the AI.
 */
@Entity(name = "AIChatMessage")
@Table(name = "ai_chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AIConversation conversation;

    @Column(name = "sender", nullable = false)
    private String sender; // "USER" or "AI"

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "intent")
    private String intent;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "token_count")
    private Integer tokenCount;
}
