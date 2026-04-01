package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySavingPlanAdviceResponse {

    private MonthlySavingPlanResponse plan;
    private Advice advice;
    private String advisorSource;
    private FailureMetadata failure;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Advice {
        private String shortSummary;

        @Builder.Default
        private List<String> actionableSuggestions = Collections.emptyList();

        @Builder.Default
        private List<String> riskWarnings = Collections.emptyList();

        private String friendlyTone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureMetadata {
        private String source;
        private String reasonType;
        private String path;
        private Long elapsedMs;
        private Long timeoutMs;
        private String message;
    }
}
