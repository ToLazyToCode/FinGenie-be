package fingenie.com.fingenie.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatRequest {
    
    private Long conversationId; // null for new conversation
    
    @NotBlank(message = "Message cannot be empty")
    private String message;
    
    private String context; // Optional additional context

    private String language; // Optional app language code (en/vi)
    
    private Boolean startNewConversation;
}
