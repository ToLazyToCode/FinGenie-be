package fingenie.com.fingenie.service;

import fingenie.com.fingenie.ai.client.AIClient;
import fingenie.com.fingenie.ai.client.dto.InsightRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialSummaryService {

    private static final int MAX_SUMMARY_LENGTH = 140;

    private final AIClient aiClient;

    @Value("${ai.summary.enabled:true}")
    private boolean summaryEnabled;

    @Value("${ai.summary.fallback-on-error:true}")
    private boolean fallbackOnError;

    public String buildHomeSummary(
            Long accountId,
            BigDecimal baseline,
            BigDecimal predictionAccuracyRate,
            String fallbackSummary
    ) {
        if (!summaryEnabled) {
            return fallbackSummary;
        }

        String message = "Generate one short sentence (max 20 words) for personal finance home dashboard summary.";
        if (baseline != null) {
            message += " Baseline daily spend: " + baseline;
        }
        if (predictionAccuracyRate != null) {
            message += " Prediction accuracy ratio: " + predictionAccuracyRate;
        }

        try {
            Map<String, Object> response = aiClient.insight(InsightRequest.builder()
                    .accountId(accountId)
                    .insightType("HOME_FINANCIAL_SUMMARY")
                    .message(message)
                    .build());

            String candidate = readString(response, "summary", "insight", "message", "text", "response");
            if (candidate == null || candidate.isBlank()) {
                return fallbackSummary;
            }
            return normalizeSummary(candidate);
        } catch (Exception ex) {
            if (!fallbackOnError) {
                throw ex;
            }
            log.warn("Failed to generate AI home summary for accountId={}, fallback applied: {}", accountId, ex.getMessage());
            return fallbackSummary;
        }
    }

    private String normalizeSummary(String value) {
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= MAX_SUMMARY_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
    }

    private String readString(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                String text = value.toString().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }
}
