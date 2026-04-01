package fingenie.com.fingenie.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private Long accountId;
    private Long conversationId;
    private String message;
    private String context;
    private String language;
    private Boolean startNewConversation;
}
