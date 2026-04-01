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
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String body;
    private Boolean isRead;
    private String actionType;
    private String actionData;
    private Integer priority;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Timestamp createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
}
