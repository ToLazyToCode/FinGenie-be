package fingenie.com.fingenie.ai.builder;

import fingenie.com.fingenie.ai.entity.PromptTemplate;
import fingenie.com.fingenie.ai.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptTemplateRepository templateRepository;
    private final ContextBuilder contextBuilder;

    /**
     * Build a prompt for a given category using available templates and injected context.
     * This method MUST never include raw sensitive data; ContextBuilder is responsible for sanitization.
     */
    public String buildPrompt(String category, Long accountId, List<String> extras) {
        PromptTemplate template = templateRepository.findByCategoryAndIsActiveTrue(category)
                .stream().findFirst()
                .orElse(PromptTemplate.builder().templateText("{context}\n{extras}").build());

        String userSummary = contextBuilder.buildFinancialSummary(accountId);
        StringBuilder extrasSb = new StringBuilder();
        if (extras != null) {
            extras.forEach(e -> {
                if (e != null && !e.isBlank()) {
                    extrasSb.append(e.replaceAll("[\r\n]", " ")).append("\n");
                }
            });
        }

        String prompt = template.getTemplateText();
        prompt = prompt.replace("{context}", userSummary == null ? "" : userSummary.replaceAll("[\r\n]+", " "))
                       .replace("{extras}", extrasSb.toString().trim());
        // Limit prompt length for safety
        if (prompt.length() > 8000) {
            return prompt.substring(0, 8000);
        }
        return prompt;
    }

    /**
     * Convenience overload: build prompt when the caller already has the summarized context.
     */
    public String buildPromptFromSummary(String category, String summary, List<String> extras) {
        PromptTemplate template = templateRepository.findByCategoryAndIsActiveTrue(category)
                .stream().findFirst()
                .orElse(PromptTemplate.builder().templateText("{context}\n{extras}").build());

        StringBuilder extrasSb = new StringBuilder();
        if (extras != null) {
            extras.forEach(e -> {
                if (e != null && !e.isBlank()) {
                    extrasSb.append(e.replaceAll("[\r\n]", " ")).append("\n");
                }
            });
        }

        String prompt = template.getTemplateText();
        prompt = prompt.replace("{context}", summary == null ? "" : summary.replaceAll("[\r\n]+", " "))
                       .replace("{extras}", extrasSb.toString().trim());
        if (prompt.length() > 8000) return prompt.substring(0, 8000);
        return prompt;
    }
}
