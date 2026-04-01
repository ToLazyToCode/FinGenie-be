package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeviceTokenResponse {
    private Long id;
    private Long accountId;
    private String deviceToken;
    private String platform;
    private Boolean enabled;
    private Timestamp lastSeenAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
