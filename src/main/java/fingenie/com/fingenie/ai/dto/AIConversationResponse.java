package fingenie.com.fingenie.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIConversationResponse {
    
    private Long id;
    private Long accountId;
    private String title;
    private Boolean isActive;
    private String contextSummary;
    private Integer totalTokens;
    private Integer messageCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<MessageResponse> recentMessages;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private Long id;
        private String sender;
        private String text;
        private Double confidence;
        private String intent;
        private String modelUsed;
        private Integer tokenCount;
        private Timestamp createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationList {
        private List<AIConversationResponse> conversations;
        private long totalCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIChatResult {
        private Long conversationId;
        private MessageResponse userMessage;
        private MessageResponse aiMessage;
        private List<String> suggestions;
        private String detectedIntent;
        private FailureMetadata failure;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureMetadata {
        private String source;
        private String reasonType;
        private String path;
        private Long elapsedMs;
        private Long timeoutMs;
        private String message;
    }
}
