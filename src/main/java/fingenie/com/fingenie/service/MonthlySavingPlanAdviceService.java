package fingenie.com.fingenie.service;

import fingenie.com.fingenie.ai.client.AIClient;
import fingenie.com.fingenie.ai.client.AIClientException;
import fingenie.com.fingenie.dto.MonthlySavingPlanAdviceResponse;
import fingenie.com.fingenie.dto.MonthlySavingPlanResponse;
import fingenie.com.fingenie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MonthlySavingPlanAdviceService {

    private static final String ADVISOR_SOURCE_PYTHON = "python";
    private static final String ADVISOR_SOURCE_FALLBACK = "fallback";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int LOOKBACK_DAYS = 30;
    private static final int TOP_CATEGORY_LIMIT = 3;

    private final MonthlySavingPlanService monthlySavingPlanService;
    private final TransactionRepository transactionRepository;
    private final AIClient aiClient;

    public MonthlySavingPlanAdviceResponse getMonthlySavingPlanAdvice(Long accountId, String requestedLanguage) {
        String languageCode = resolveLanguageCode(requestedLanguage);
        MonthlySavingPlanResponse plan = monthlySavingPlanService.getMonthlySavingPlan(accountId, "optimized");
        List<Map<String, Object>> categorySpend = loadCategorySpend(accountId);
        Map<String, Object> payload = buildAdvicePayload(plan, categorySpend, languageCode);

        try {
            Map<String, Object> aiResponse = aiClient.savingPlanAdvice(payload);
            MonthlySavingPlanAdviceResponse.Advice fallback = buildFallbackAdvice(plan, categorySpend, languageCode);
            MonthlySavingPlanAdviceResponse.Advice advice = mapAdvice(aiResponse, fallback);
            return MonthlySavingPlanAdviceResponse.builder()
                    .plan(plan)
                    .advice(advice)
                    .advisorSource(ADVISOR_SOURCE_PYTHON)
                    .build();
        } catch (Exception ex) {
            MonthlySavingPlanAdviceResponse.FailureMetadata failure = mapFailureMetadata(ex);
            log.warn(
                    "Failed to get AI saving-plan advice, using fallback accountId={} language={} reasonType={} path={} elapsedMs={} timeoutMs={} message={}",
                    accountId,
                    languageCode,
                    failure.getReasonType(),
                    failure.getPath(),
                    failure.getElapsedMs(),
                    failure.getTimeoutMs(),
                    failure.getMessage()
            );
            log.debug("Saving-plan advice fallback stacktrace accountId={} language={}", accountId, languageCode, ex);
            MonthlySavingPlanAdviceResponse.Advice advice = buildFallbackAdvice(plan, categorySpend, languageCode);
            return MonthlySavingPlanAdviceResponse.builder()
                    .plan(plan)
                    .advice(advice)
                    .advisorSource(ADVISOR_SOURCE_FALLBACK)
                    .failure(failure)
                    .build();
        }
    }

    private Map<String, Object> buildAdvicePayload(
            MonthlySavingPlanResponse plan,
            List<Map<String, Object>> categorySpend,
            String languageCode
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String languageName = "vi".equals(languageCode) ? "Vietnamese" : "English";
        payload.put("saving_capacity", normalizeMoney(plan.getSavingCapacity()));
        payload.put("targets", buildTargets(plan));
        payload.put("allocations", buildAllocations(plan));
        payload.put("recommendations", buildRecommendations(plan));
        payload.put("category_spend", categorySpend);
        payload.put("app_language", languageCode);
        payload.put("preferred_response_language", languageName);
        payload.put("response_language_strict", "Reply only in " + languageName);
        return payload;
    }

    private List<Map<String, Object>> buildTargets(MonthlySavingPlanResponse plan) {
        List<Map<String, Object>> targets = new ArrayList<>();
        if (plan.getAllocations() == null) {
            return targets;
        }
        for (MonthlySavingPlanResponse.Allocation allocation : plan.getAllocations()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", allocation.getType() == null ? "" : allocation.getType().name());
            item.put("id", allocation.getId());
            item.put("title", allocation.getTitle());
            item.put("required_monthly", normalizeMoney(allocation.getRequiredMonthly()));
            item.put("notes", allocation.getNotes());
            targets.add(item);
        }
        return targets;
    }

    private List<Map<String, Object>> buildAllocations(MonthlySavingPlanResponse plan) {
        List<Map<String, Object>> allocations = new ArrayList<>();
        if (plan.getAllocations() == null) {
            return allocations;
        }
        for (MonthlySavingPlanResponse.Allocation allocation : plan.getAllocations()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", allocation.getType() == null ? "" : allocation.getType().name());
            item.put("id", allocation.getId());
            item.put("title", allocation.getTitle());
            item.put("required_monthly", normalizeMoney(allocation.getRequiredMonthly()));
            item.put("allocated_monthly", normalizeMoney(allocation.getAllocatedMonthly()));
            item.put("feasibility_score", normalizeMoney(allocation.getFeasibilityScore()));
            allocations.add(item);
        }
        return allocations;
    }

    private List<Map<String, Object>> buildRecommendations(MonthlySavingPlanResponse plan) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        if (plan.getRecommendations() == null) {
            return recommendations;
        }
        for (MonthlySavingPlanResponse.Recommendation recommendation : plan.getRecommendations()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", recommendation.getType());
            item.put("message", recommendation.getMessage());
            item.put("impact_monthly", normalizeMoney(recommendation.getImpactMonthly()));
            recommendations.add(item);
        }
        return recommendations;
    }

    private List<Map<String, Object>> loadCategorySpend(Long accountId) {
        LocalDate today = LocalDate.now();
        Date startDate = Date.valueOf(today.minusDays(LOOKBACK_DAYS - 1L));
        Date endDate = Date.valueOf(today);

        List<Object[]> rows = transactionRepository.sumExpenseByCategory(accountId, startDate, endDate);
        List<Map<String, Object>> items = new ArrayList<>();

        for (Object[] row : rows) {
            if (row == null || row.length < 3) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category_id", row[0]);
            item.put("category_name", readString(row[1], "Uncategorized"));
            item.put("monthly_expense", normalizeMoney(readMoney(row[2])));
            items.add(item);
            if (items.size() >= TOP_CATEGORY_LIMIT) {
                break;
            }
        }

        return items;
    }

    private MonthlySavingPlanAdviceResponse.Advice mapAdvice(
            Map<String, Object> aiResponse,
            MonthlySavingPlanAdviceResponse.Advice fallback
    ) {
        String summary = readString(aiResponse.get("short_summary"), fallback.getShortSummary());
        String tone = readString(aiResponse.get("friendly_tone"), fallback.getFriendlyTone());

        List<String> fallbackSuggestions = fallback.getActionableSuggestions() == null
                ? List.of()
                : fallback.getActionableSuggestions();
        String fillSuggestion = fallbackSuggestions.isEmpty()
                ? "Keep a weekly spending check-in to protect your saving plan."
                : fallbackSuggestions.get(0);
        List<String> suggestions = normalizeSuggestions(
                readStringList(aiResponse.get("actionable_suggestions")),
                fallbackSuggestions,
                fillSuggestion
        );

        List<String> riskWarnings = readStringList(aiResponse.get("risk_warnings"));
        if (riskWarnings.isEmpty()) {
            riskWarnings = fallback.getRiskWarnings() == null ? List.of() : fallback.getRiskWarnings();
        }

        return MonthlySavingPlanAdviceResponse.Advice.builder()
                .shortSummary(summary)
                .friendlyTone(tone)
                .actionableSuggestions(suggestions)
                .riskWarnings(riskWarnings)
                .build();
    }

    private MonthlySavingPlanAdviceResponse.Advice buildFallbackAdvice(
            MonthlySavingPlanResponse plan,
            List<Map<String, Object>> categorySpend,
            String languageCode
    ) {
        boolean isVietnamese = "vi".equals(languageCode);
        BigDecimal savingCapacity = normalizeMoney(plan.getSavingCapacity());
        BigDecimal totalRequired = normalizeMoney(plan.getTotalRequired());
        BigDecimal gap = totalRequired.subtract(savingCapacity).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal feasibility = BigDecimal.valueOf(100);
        if (totalRequired.compareTo(BigDecimal.ZERO) > 0) {
            feasibility = savingCapacity.multiply(BigDecimal.valueOf(100))
                    .divide(totalRequired, 2, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100));
        }

        String shortSummary;
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            shortSummary = isVietnamese
                    ? "Ban dang dap ung khoang " + feasibility.toPlainString()
                    + "% muc tieu tiet kiem thang nay, con thieu " + gap.toPlainString() + "."
                    : "You are currently funding " + feasibility.toPlainString()
                    + "% of this month's saving targets, short by " + gap.toPlainString() + ".";
        } else {
            shortSummary = isVietnamese
                    ? "Rat tot. Nang luc tiet kiem hien tai da bao phu day du cac muc phan bo thang nay."
                    : "Great work. Your current saving capacity fully covers this month's target allocations.";
        }

        List<String> suggestions = new ArrayList<>();
        if (plan.getRecommendations() != null) {
            for (MonthlySavingPlanResponse.Recommendation recommendation : plan.getRecommendations()) {
                if (recommendation != null && recommendation.getMessage() != null && !recommendation.getMessage().isBlank()) {
                    suggestions.add(recommendation.getMessage().trim());
                }
                if (suggestions.size() >= 3) {
                    break;
                }
            }
        }

        for (Map<String, Object> category : categorySpend) {
            if (suggestions.size() >= 3) {
                break;
            }
            String name = readString(category.get("category_name"), isVietnamese ? "Khong phan loai" : "Uncategorized");
            BigDecimal monthlyExpense = normalizeMoney(readMoney(category.get("monthly_expense")));
            BigDecimal cut10 = monthlyExpense.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            suggestions.add(isVietnamese
                    ? "Giam chi tieu danh muc " + name + " 10% de tiet kiem them khoang " + cut10.toPlainString() + " moi thang."
                    : "Reduce " + name + " spending by 10% to free about " + cut10.toPlainString() + " monthly.");
        }

        List<String> defaults = isVietnamese
                ? List.of(
                "Uu tien muc tieu HIGH truoc va tam hoan muc tieu LOW khi can.",
                "Theo doi chi tieu hang ngay o danh muc lon nhat de tranh vuot ke hoach cuoi thang.",
                "Bat chuyen tien tiet kiem tu dong sau khi nhan thu nhap de bao ve ke hoach thang."
        )
                : List.of(
                "Prioritize HIGH priority targets first and defer LOW priority targets when needed.",
                "Track daily spending in your top category to avoid month-end surprises.",
                "Set an automatic post-income transfer to savings to protect your monthly plan."
        );
        int index = 0;
        while (suggestions.size() < 3 && index < defaults.size()) {
            suggestions.add(defaults.get(index));
            index++;
        }

        List<String> riskWarnings = new ArrayList<>();
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            riskWarnings.add(isVietnamese
                    ? "Phat hien khoang thieu muc tieu tiet kiem thang: " + gap.toPlainString()
                    : "Monthly saving target gap detected: " + gap.toPlainString());
        }
        if (savingCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            riskWarnings.add(isVietnamese
                    ? "Nang luc tiet kiem hien tai bang 0 hoac am."
                    : "Current saving capacity is zero or negative.");
        }
        if (plan.getAllocations() != null) {
            boolean lowFeasibility = plan.getAllocations().stream()
                    .anyMatch(allocation -> normalizeMoney(allocation.getFeasibilityScore()).compareTo(BigDecimal.valueOf(50)) < 0);
            if (lowFeasibility) {
                riskWarnings.add(isVietnamese
                        ? "Mot so muc tieu duoi 50% kha thi trong thang nay."
                        : "Some targets are below 50% feasibility this month.");
            }
        }

        String friendlyTone;
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            friendlyTone = isVietnamese
                    ? "Thu cung FinGenie dang co vu ban. Chi can mot dieu chinh chi tieu thong minh trong tuan nay la co the thu hep khoang thieu nhanh hon."
                    : "Your FinGenie pet is rooting for you. One smart spending cut this week can close the gap faster.";
        } else {
            friendlyTone = isVietnamese
                    ? "Thu cung FinGenie rat tu hao ve ban. Giu nhip do nay va cac muc tieu se dung tien do."
                    : "Your FinGenie pet is proud of you. Keep this steady rhythm and your goals stay on track.";
        }

        return MonthlySavingPlanAdviceResponse.Advice.builder()
                .shortSummary(shortSummary)
                .actionableSuggestions(normalizeSuggestions(suggestions, defaults, defaults.get(0)))
                .riskWarnings(riskWarnings)
                .friendlyTone(friendlyTone)
                .build();
    }

    private List<String> normalizeSuggestions(List<String> suggestions, List<String> fallback, String fillSuggestion) {
        List<String> result = new ArrayList<>();
        if (suggestions != null) {
            for (String suggestion : suggestions) {
                if (suggestion != null && !suggestion.isBlank() && !result.contains(suggestion.trim())) {
                    result.add(suggestion.trim());
                }
                if (result.size() >= 3) {
                    break;
                }
            }
        }
        if (fallback != null) {
            for (String suggestion : fallback) {
                if (suggestion != null && !suggestion.isBlank() && !result.contains(suggestion.trim())) {
                    result.add(suggestion.trim());
                }
                if (result.size() >= 3) {
                    break;
                }
            }
        }
        while (result.size() < 3) {
            result.add(fillSuggestion);
        }
        return result.subList(0, 3);
    }

    private List<String> readStringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item == null) {
                continue;
            }
            String text = item.toString().trim();
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return result;
    }

    private BigDecimal readMoney(Object raw) {
        if (raw == null) {
            return ZERO;
        }
        if (raw instanceof BigDecimal value) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
        if (raw instanceof Number value) {
            return BigDecimal.valueOf(value.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(raw.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return ZERO;
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveLanguageCode(String requestedLanguage) {
        String normalized = requestedLanguage == null
                ? ""
                : requestedLanguage.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("vi") || normalized.contains("vietnam")) {
            return "vi";
        }
        return "en";
    }

    private MonthlySavingPlanAdviceResponse.FailureMetadata mapFailureMetadata(Throwable throwable) {
        AIClientException aiClientException = findAIClientException(throwable);
        if (aiClientException != null) {
            return MonthlySavingPlanAdviceResponse.FailureMetadata.builder()
                    .source(ADVISOR_SOURCE_FALLBACK)
                    .reasonType(aiClientException.getFailureType().name())
                    .path(aiClientException.getPath())
                    .elapsedMs(aiClientException.getElapsedMs())
                    .timeoutMs(aiClientException.getTimeoutMs())
                    .message(aiClientException.getMessage())
                    .build();
        }

        Throwable root = throwable;
        while (root != null && root.getCause() != null) {
            root = root.getCause();
        }
        return MonthlySavingPlanAdviceResponse.FailureMetadata.builder()
                .source(ADVISOR_SOURCE_FALLBACK)
                .reasonType(root == null ? "UNKNOWN_ERROR" : root.getClass().getSimpleName())
                .path("/ai/saving-plan/advice")
                .elapsedMs(null)
                .timeoutMs(null)
                .message(root == null ? "unknown" : root.getMessage())
                .build();
    }

    private AIClientException findAIClientException(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof AIClientException aiClientException) {
                return aiClientException;
            }
            cursor = cursor.getCause();
        }
        return null;
    }

    private String readString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return fallback;
        }
        return text;
    }
}
