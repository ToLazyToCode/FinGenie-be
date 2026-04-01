package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class XPLogResponse {

    private Long xpLogId;
    private Long accountId;
    private String sourceType;
    private Long sourceId;
    private Integer xpAmount;
    private String description;
    private LocalDateTime createdAt;
}
