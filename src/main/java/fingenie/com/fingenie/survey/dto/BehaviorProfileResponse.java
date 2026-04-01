package fingenie.com.fingenie.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for user behavior profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorProfileResponse {
    private Long userId;
    private Integer profileVersion;
    
    // Scores (0-100)
    private BigDecimal overspendingScore;
    private BigDecimal debtRiskScore;
    private BigDecimal savingsCapacityScore;
    private BigDecimal financialAnxietyIndex;
    
    // Segment
    private String segment;
    private String segmentDisplayName;
    private String segmentDescription;
    private BigDecimal segmentConfidence;
    
    // Risk assessment
    private String riskLevel;
    
    // Explainability
    private List<String> topFactors;
    private List<ActionItem> suggestedActions;
    
    // Timestamps
    private LocalDateTime surveyCompletedAt;
    private LocalDateTime profileUpdatedAt;
    private Boolean needsRefresh;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String title;
        private String description;
        private String priority; // HIGH, MEDIUM, LOW
        private String category; // SPENDING, SAVING, DEBT, PLANNING
    }
}
