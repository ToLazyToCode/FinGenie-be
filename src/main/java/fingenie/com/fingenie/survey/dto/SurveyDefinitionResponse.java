package fingenie.com.fingenie.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for survey definition with questions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDefinitionResponse {
    private Long id;
    private Long version;
    private String title;
    private String description;
    private Integer estimatedMinutes;
    private List<SectionResponse> sections;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionResponse {
        private String code;
        private String title;
        private Integer order;
        private List<QuestionResponse> questions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResponse {
        private Long id;
        private String questionCode;
        private String questionText;
        private Integer order;
        private Boolean isRequired;
        private List<AnswerOptionResponse> options;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerOptionResponse {
        private String code;
        private String text;
        private Integer order;
    }
}
