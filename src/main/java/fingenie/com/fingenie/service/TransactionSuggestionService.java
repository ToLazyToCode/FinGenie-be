package fingenie.com.fingenie.service;

import fingenie.com.fingenie.dto.TransactionSuggestionResponse;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.SpendingGuess;
import fingenie.com.fingenie.repository.SpendingGuessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionSuggestionService {

    private final SpendingGuessRepository spendingGuessRepository;

    @Transactional(readOnly = true)
    public Optional<TransactionSuggestionResponse> getTodaySuggestion(Long accountId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return spendingGuessRepository.findTodayGuesses(accountId, startOfDay, endOfDay).stream()
                .filter(SpendingGuess::isPending)
                .filter(guess -> !guess.isExpired())
                .max(Comparator.comparing(SpendingGuess::getConfidence, Comparator.nullsLast(BigDecimal::compareTo))
                        .thenComparing(SpendingGuess::getGuessedForTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse);
    }

    private TransactionSuggestionResponse toResponse(SpendingGuess guess) {
        String transactionType = resolveTransactionType(guess);

        return TransactionSuggestionResponse.builder()
                .predictionId(guess.getId())
                .transactionType(transactionType)
                .type(transactionType)
                .categoryId(guess.getCategory() != null ? guess.getCategory().getId() : null)
                .categoryName(guess.getCategory() != null ? guess.getCategory().getCategoryName() : null)
                .amount(guess.getGuessedAmount())
                .note(null)
                .reason(guess.getReasoning())
                .confidence(guess.getConfidence() != null ? guess.getConfidence().doubleValue() : null)
                .build();
    }

    private String resolveTransactionType(SpendingGuess guess) {
        if (guess.getCategory() != null && guess.getCategory().getCategoryType() == Category.CategoryType.INCOME) {
            return "INCOME";
        }
        return "EXPENSE";
    }
}

