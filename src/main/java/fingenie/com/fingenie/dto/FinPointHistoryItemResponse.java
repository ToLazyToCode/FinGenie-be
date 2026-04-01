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
public class FinPointHistoryItemResponse {
    private Long id;
    private Long amount;
    private String sourceType;
    private String sourceRefType;
    private String sourceRefId;
    private String reason;
    private String missionId;
    private String missionDay;
    private Timestamp createdAt;
}
