package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @NotNull(message = "Conversation ID is required")
    private Long conversationId;

    @NotBlank(message = "Content is required")
    private String content;

    private String messageType; // TEXT, IMAGE, EMOJI, ACHIEVEMENT_SHARE, STREAK_SHARE

    private String attachmentUrl;
}
