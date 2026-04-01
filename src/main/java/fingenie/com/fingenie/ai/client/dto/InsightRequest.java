package fingenie.com.fingenie.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightRequest {
    private Long accountId;
    private String insightType;
    private String message;
    private Map<String, Object> featureSnapshot;
    private List<Map<String, Object>> recentTransactions;
    private String userSegment;
}
