package fingenie.com.fingenie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private String messageType;
    private Boolean isRead;
    private String attachmentUrl;
    private Boolean isMine;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Timestamp createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime readAt;
}
