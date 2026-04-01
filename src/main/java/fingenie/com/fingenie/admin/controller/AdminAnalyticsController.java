package fingenie.com.fingenie.admin.controller;

import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.Transaction;
import fingenie.com.fingenie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin-only analytics endpoints – aggregated platform-wide, no entitlement check.
 *
 * GET /api/v1/admin/analytics/overview?period=weekly|monthly|yearly
 */
@RestController
@RequestMapping("${api-prefix}/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final TransactionRepository transactionRepository;

    // ── Response records ────────────────────────────────────────────────────

    public record CategoryBreakdownDto(
            Long   categoryId,
            String categoryName,
            double amount,
            double percentage,
            String type        // "INCOME" | "EXPENSE"
    ) {}

    public record AnalyticsOverviewDto(
            double totalIncome,
            double totalExpense,
            double netSavings,
            List<CategoryBreakdownDto> categories
    ) {}

    // ── Endpoint ────────────────────────────────────────────────────────────

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewDto> overview(
            @RequestParam(defaultValue = "weekly") String period
    ) {
        LocalDate end   = LocalDate.now();
        LocalDate start = switch (period.toLowerCase()) {
            case "monthly" -> end.minusMonths(1);
            case "yearly"  -> end.minusYears(1);
            default        -> end.minusWeeks(1);   // weekly
        };

        Date startDate = Date.from(start.atStartOfDay().toInstant(ZoneOffset.UTC));
        Date endDate   = Date.from(end.atTime(23, 59, 59).toInstant(ZoneOffset.UTC));

        // All transactions across all users in the period
        List<Transaction> txns = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate() != null
                          && !t.getTransactionDate().before(startDate)
                          && !t.getTransactionDate().after(endDate))
                .collect(Collectors.toList());

        double totalIncome  = txns.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

        double totalExpense = txns.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                .sum();

        double netSavings = totalIncome - totalExpense;

        // Group by category
        Map<Long, List<Transaction>> byCat = txns.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(t -> t.getCategory().getId()));

        double denomIncome  = totalIncome  == 0 ? 1 : totalIncome;
        double denomExpense = totalExpense == 0 ? 1 : totalExpense;

        List<CategoryBreakdownDto> categories = byCat.entrySet().stream()
                .map(entry -> {
                    List<Transaction> catTxns = entry.getValue();
                    Category cat = catTxns.get(0).getCategory();

                    boolean isIncome = cat.getCategoryType() == Category.CategoryType.INCOME;

                    double catAmount = catTxns.stream()
                            .mapToDouble(t -> Math.abs(t.getAmount().doubleValue()))
                            .sum();

                    double pct = isIncome
                            ? (catAmount / denomIncome  * 100)
                            : (catAmount / denomExpense * 100);

                    double pctRounded = BigDecimal.valueOf(pct)
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();

                    return new CategoryBreakdownDto(
                            cat.getId(),
                            cat.getCategoryName(),
                            catAmount,
                            pctRounded,
                            isIncome ? "INCOME" : "EXPENSE"
                    );
                })
                .sorted(Comparator.comparingDouble(CategoryBreakdownDto::amount).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AnalyticsOverviewDto(
                totalIncome,
                totalExpense,
                netSavings,
                categories
        ));
    }
}
