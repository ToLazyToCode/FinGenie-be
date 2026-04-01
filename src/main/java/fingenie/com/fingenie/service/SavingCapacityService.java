package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.SavingCapacityResponse;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.Transaction;
import fingenie.com.fingenie.repository.TransactionRepository;
import fingenie.com.fingenie.survey.entity.UserSurveyResponse;
import fingenie.com.fingenie.survey.enums.SurveyStatus;
import fingenie.com.fingenie.survey.repository.UserSurveyResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingCapacityService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int PRIMARY_EXPENSE_WINDOW_DAYS = 30;
    private static final int FALLBACK_EXPENSE_WINDOW_DAYS = 60;
    private static final int INCOME_LOOKBACK_DAYS = 90;
    private static final int MIN_EXPENSE_SAMPLE_COUNT = 6;
    private static final List<String> SALARY_LIKE_KEYWORDS = List.of(
            "salary", "payroll", "paycheck", "wage", "bonus", "income"
    );

    private final TransactionRepository transactionRepository;
    private final UserSurveyResponseRepository userSurveyResponseRepository;

    @Transactional(readOnly = true)
    public SavingCapacityResponse calculateSavingCapacity(Long accountId) {
        ExpenseEstimate expenseEstimate = estimateMonthlyExpenseInternal(accountId);
        IncomeEstimate incomeEstimate = estimateMonthlyIncomeInternal(accountId, expenseEstimate.monthlyExpense());

        BigDecimal savingCapacity = incomeEstimate.monthlyIncome()
                .subtract(expenseEstimate.monthlyExpense())
                .max(ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal expenseRatio = calculateExpenseRatio(
                expenseEstimate.monthlyExpense(),
                incomeEstimate.monthlyIncome()
        );

        SavingCapacityResponse.ConfidenceLevel confidence = mergeConfidence(
                incomeEstimate.confidence(),
                expenseEstimate.confidence()
        );

        return SavingCapacityResponse.builder()
                .monthlyIncome(incomeEstimate.monthlyIncome())
                .monthlyExpense(expenseEstimate.monthlyExpense())
                .savingCapacity(savingCapacity)
                .expenseRatio(expenseRatio)
                .confidence(confidence)
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal estimateMonthlyIncome(Long accountId) {
        BigDecimal monthlyExpense = estimateMonthlyExpense(accountId);
        return estimateMonthlyIncomeInternal(accountId, monthlyExpense).monthlyIncome();
    }

    @Transactional(readOnly = true)
    public BigDecimal estimateMonthlyExpense(Long accountId) {
        return estimateMonthlyExpenseInternal(accountId).monthlyExpense();
    }

    private IncomeEstimate estimateMonthlyIncomeInternal(Long accountId, BigDecimal monthlyExpense) {
        Optional<IncomeEstimate> surveyEstimate = estimateIncomeFromSurvey(accountId, monthlyExpense);
        if (surveyEstimate.isPresent()) {
            return surveyEstimate.get();
        }
        return estimateIncomeFromTransactions(accountId);
    }

    private Optional<IncomeEstimate> estimateIncomeFromSurvey(Long accountId, BigDecimal monthlyExpense) {
        if (monthlyExpense == null || monthlyExpense.compareTo(ZERO) <= 0) {
            return Optional.empty();
        }

        Optional<UserSurveyResponse> latestSurvey = userSurveyResponseRepository
                .findByUserIdAndIsLatestTrueAndStatus(accountId, SurveyStatus.COMPLETED);

        if (latestSurvey.isEmpty() || latestSurvey.get().getAnswers() == null) {
            return Optional.empty();
        }

        Map<String, String> answers = latestSurvey.get().getAnswers();
        BigDecimal fixedExpenseRatio = mapFixedExpenseRatio(answers.get("A3"));
        if (fixedExpenseRatio == null || fixedExpenseRatio.compareTo(ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal estimatedIncome = monthlyExpense
                .divide(fixedExpenseRatio, 2, RoundingMode.HALF_UP);

        SavingCapacityResponse.ConfidenceLevel confidence =
                mapSurveyConfidence(answers.get("A2"));

        return Optional.of(new IncomeEstimate(estimatedIncome, confidence));
    }

    private IncomeEstimate estimateIncomeFromTransactions(Long accountId) {
        LocalDate today = LocalDate.now();
        Date endDate = Date.valueOf(today);
        Date startDate = Date.valueOf(today.minusDays(INCOME_LOOKBACK_DAYS - 1L));

        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
        List<Transaction> incomeTransactions = transactions.stream()
                .filter(this::isIncomeTransaction)
                .collect(Collectors.toList());

        if (incomeTransactions.isEmpty()) {
            return new IncomeEstimate(ZERO.setScale(2, RoundingMode.HALF_UP), SavingCapacityResponse.ConfidenceLevel.LOW);
        }

        List<Transaction> salaryLikeTransactions = incomeTransactions.stream()
                .filter(this::isSalaryLike)
                .collect(Collectors.toList());

        if (!salaryLikeTransactions.isEmpty()) {
            BigDecimal monthly = averageMonthlyIncome(salaryLikeTransactions);
            int activeMonths = countActiveMonths(salaryLikeTransactions);
            SavingCapacityResponse.ConfidenceLevel confidence =
                    activeMonths >= 2 ? SavingCapacityResponse.ConfidenceLevel.HIGH : SavingCapacityResponse.ConfidenceLevel.MEDIUM;
            return new IncomeEstimate(monthly, confidence);
        }

        BigDecimal monthly = averageMonthlyIncome(incomeTransactions);
        int activeMonths = countActiveMonths(incomeTransactions);
        SavingCapacityResponse.ConfidenceLevel confidence =
                activeMonths >= 2 ? SavingCapacityResponse.ConfidenceLevel.MEDIUM : SavingCapacityResponse.ConfidenceLevel.LOW;
        return new IncomeEstimate(monthly, confidence);
    }

    private ExpenseEstimate estimateMonthlyExpenseInternal(Long accountId) {
        LocalDate today = LocalDate.now();
        LocalDate start60 = today.minusDays(FALLBACK_EXPENSE_WINDOW_DAYS - 1L);
        LocalDate start30 = today.minusDays(PRIMARY_EXPENSE_WINDOW_DAYS - 1L);

        List<Transaction> transactions60 = transactionRepository.findByAccountIdAndDateRange(
                accountId,
                Date.valueOf(start60),
                Date.valueOf(today)
        );

        List<Transaction> expenseTransactions60 = transactions60.stream()
                .filter(this::isExpenseTransaction)
                .collect(Collectors.toList());
        List<Transaction> expenseTransactions30 = expenseTransactions60.stream()
                .filter(tx -> !tx.getTransactionDate().toLocalDate().isBefore(start30))
                .collect(Collectors.toList());

        BigDecimal total30 = sumAbsoluteAmount(expenseTransactions30);
        BigDecimal total60 = sumAbsoluteAmount(expenseTransactions60);

        if (expenseTransactions30.size() >= MIN_EXPENSE_SAMPLE_COUNT) {
            return new ExpenseEstimate(scale(total30), SavingCapacityResponse.ConfidenceLevel.HIGH);
        }

        if (expenseTransactions60.size() >= MIN_EXPENSE_SAMPLE_COUNT) {
            BigDecimal monthlyAverage = total60
                    .multiply(BigDecimal.valueOf(30))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            return new ExpenseEstimate(scale(monthlyAverage), SavingCapacityResponse.ConfidenceLevel.MEDIUM);
        }

        if (!expenseTransactions30.isEmpty()) {
            return new ExpenseEstimate(scale(total30), SavingCapacityResponse.ConfidenceLevel.LOW);
        }

        if (!expenseTransactions60.isEmpty()) {
            BigDecimal monthlyAverage = total60
                    .multiply(BigDecimal.valueOf(30))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            return new ExpenseEstimate(scale(monthlyAverage), SavingCapacityResponse.ConfidenceLevel.LOW);
        }

        return new ExpenseEstimate(ZERO.setScale(2, RoundingMode.HALF_UP), SavingCapacityResponse.ConfidenceLevel.LOW);
    }

    private BigDecimal averageMonthlyIncome(List<Transaction> incomeTransactions) {
        Map<YearMonth, BigDecimal> monthlyIncome = new HashMap<>();

        for (Transaction tx : incomeTransactions) {
            if (tx.getTransactionDate() == null || tx.getAmount() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(tx.getTransactionDate().toLocalDate());
            BigDecimal amount = tx.getAmount().abs();
            monthlyIncome.merge(month, amount, BigDecimal::add);
        }

        if (monthlyIncome.isEmpty()) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = monthlyIncome.values().stream().reduce(ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(monthlyIncome.size()), 2, RoundingMode.HALF_UP);
    }

    private int countActiveMonths(List<Transaction> transactions) {
        return (int) transactions.stream()
                .filter(tx -> tx.getTransactionDate() != null)
                .map(tx -> YearMonth.from(tx.getTransactionDate().toLocalDate()))
                .distinct()
                .count();
    }

    private boolean isSalaryLike(Transaction tx) {
        String categoryName = normalize(tx.getCategory() != null ? tx.getCategory().getCategoryName() : null);
        String description = normalize(tx.getDescription());
        for (String keyword : SALARY_LIKE_KEYWORDS) {
            if (categoryName.contains(keyword) || description.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIncomeTransaction(Transaction tx) {
        if (tx == null || tx.getAmount() == null) {
            return false;
        }
        if (tx.getCategory() != null && tx.getCategory().getCategoryType() == Category.CategoryType.INCOME) {
            return true;
        }
        return tx.getAmount().compareTo(ZERO) > 0;
    }

    private boolean isExpenseTransaction(Transaction tx) {
        if (tx == null || tx.getAmount() == null) {
            return false;
        }
        if (tx.getCategory() != null && tx.getCategory().getCategoryType() == Category.CategoryType.EXPENSE) {
            return true;
        }
        return tx.getAmount().compareTo(ZERO) < 0;
    }

    private BigDecimal sumAbsoluteAmount(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .map(BigDecimal::abs)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal mapFixedExpenseRatio(String answerCode) {
        if (answerCode == null) {
            return null;
        }
        return switch (answerCode) {
            case "UNDER_30" -> new BigDecimal("0.25");
            case "BETWEEN_30_50" -> new BigDecimal("0.40");
            case "BETWEEN_50_70" -> new BigDecimal("0.60");
            case "OVER_70" -> new BigDecimal("0.80");
            default -> null;
        };
    }

    private SavingCapacityResponse.ConfidenceLevel mapSurveyConfidence(String stabilityAnswerCode) {
        if ("VERY_STABLE".equals(stabilityAnswerCode) || "MOSTLY_STABLE".equals(stabilityAnswerCode)) {
            return SavingCapacityResponse.ConfidenceLevel.HIGH;
        }
        return SavingCapacityResponse.ConfidenceLevel.MEDIUM;
    }

    private BigDecimal calculateExpenseRatio(BigDecimal expense, BigDecimal income) {
        if (income == null || income.compareTo(ZERO) <= 0) {
            return ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return expense.divide(income, 4, RoundingMode.HALF_UP);
    }

    private SavingCapacityResponse.ConfidenceLevel mergeConfidence(
            SavingCapacityResponse.ConfidenceLevel incomeConfidence,
            SavingCapacityResponse.ConfidenceLevel expenseConfidence
    ) {
        if (incomeConfidence == SavingCapacityResponse.ConfidenceLevel.LOW
                || expenseConfidence == SavingCapacityResponse.ConfidenceLevel.LOW) {
            return SavingCapacityResponse.ConfidenceLevel.LOW;
        }
        if (incomeConfidence == SavingCapacityResponse.ConfidenceLevel.HIGH
                && expenseConfidence == SavingCapacityResponse.ConfidenceLevel.HIGH) {
            return SavingCapacityResponse.ConfidenceLevel.HIGH;
        }
        return SavingCapacityResponse.ConfidenceLevel.MEDIUM;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private record IncomeEstimate(BigDecimal monthlyIncome, SavingCapacityResponse.ConfidenceLevel confidence) {}

    private record ExpenseEstimate(BigDecimal monthlyExpense, SavingCapacityResponse.ConfidenceLevel confidence) {}
}
