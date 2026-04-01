package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class XPLogRequest {

    private Long accountId;
    private String sourceType;
    private Long sourceId;
    private Integer xpAmount;
    private String description;
}
