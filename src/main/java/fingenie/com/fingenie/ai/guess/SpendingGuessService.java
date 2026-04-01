package fingenie.com.fingenie.ai.guess;

import fingenie.com.fingenie.ai.event.GuessAcceptedEvent;
import fingenie.com.fingenie.ai.event.GuessEditedEvent;
import fingenie.com.fingenie.ai.event.GuessRejectedEvent;
import fingenie.com.fingenie.ai.guess.dto.*;
import fingenie.com.fingenie.ai.service.AIPredictionEngineService;
import fingenie.com.fingenie.common.CustomException;
import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.*;
import fingenie.com.fingenie.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpendingGuessService {

    private final SpendingGuessRepository guessRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final AIPredictionEngineService predictionEngine;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_GUESSES_PER_DAY = 3;
    private static final int GUESS_EXPIRY_HOURS = 6;

    /**
     * Get today's pending guesses for current user
     */
    public List<SpendingGuessResponse> getTodayGuesses() {
        Account account = SecurityUtils.getCurrentAccount();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<SpendingGuess> guesses = guessRepository.findTodayGuesses(
                account.getId(), startOfDay, endOfDay);

        return guesses.stream()
                .filter(SpendingGuess::isPending)
                .filter(g -> !g.isExpired())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Accept a spending guess and create transaction
     */
    @Transactional
    public AcceptGuessResponse acceptGuess(Long guessId) {
        Account account = SecurityUtils.getCurrentAccount();
        SpendingGuess guess = getGuessForUser(guessId, account.getId());

        validateGuessForAction(guess);

        // Create transaction from guess
        Transaction transaction = createTransactionFromGuess(account, guess);
        Transaction saved = transactionRepository.save(transaction);

        // Update wallet balance
        updateWalletBalance(guess.getWallet(), guess.getGuessedAmount(), guess.getCategory());

        // Mark guess as accepted
        guess.markAccepted(saved.getId());
        guessRepository.save(guess);

        // Publish event for AI learning
        eventPublisher.publishEvent(new GuessAcceptedEvent(this, account.getId(), guess));

        log.info("Guess {} accepted by user {}, created transaction {}", 
                guessId, account.getId(), saved.getId());

        return AcceptGuessResponse.builder()
                .guessId(guessId)
                .transactionId(saved.getId())
                .amount(guess.getGuessedAmount())
                .category(guess.getCategory() != null ? guess.getCategory().getCategoryName() : null)
                .message("Transaction created successfully from AI prediction")
                .build();
    }

    /**
     * Accept with edits - user modifies amount/category before accepting
     */
    @Transactional
    public AcceptGuessResponse acceptWithEdit(Long guessId, EditGuessRequest editRequest) {
        Account account = SecurityUtils.getCurrentAccount();
        SpendingGuess guess = getGuessForUser(guessId, account.getId());

        validateGuessForAction(guess);

        // Get edited values
        BigDecimal finalAmount = editRequest.getAmount() != null ? 
                editRequest.getAmount() : guess.getGuessedAmount();
        
        Category finalCategory = guess.getCategory();
        if (editRequest.getCategoryId() != null) {
            finalCategory = categoryRepository.findById(editRequest.getCategoryId())
                    .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, 
                            "INVALID_CATEGORY", "Category not found"));
        }

        Wallet finalWallet = guess.getWallet();
        if (editRequest.getWalletId() != null) {
            finalWallet = walletRepository.findById(editRequest.getWalletId())
                    .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST,
                            "INVALID_WALLET", "Wallet not found"));
        }

        // Create transaction with edited values
        Transaction transaction = Transaction.builder()
                .account(account)
                .wallet(finalWallet)
                .category(finalCategory)
                .amount(finalAmount)
                .description(editRequest.getDescription() != null ? 
                        editRequest.getDescription() : "AI Guess (edited)")
                .transactionDate(java.sql.Date.valueOf(LocalDate.now()))
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // Update wallet balance
        updateWalletBalance(finalWallet, finalAmount, finalCategory);

        // Mark guess as edited
        guess.markEdited(finalAmount, 
                finalCategory != null ? finalCategory.getId() : null, 
                saved.getId());
        guessRepository.save(guess);

        // Publish event for AI learning with delta information
        eventPublisher.publishEvent(new GuessEditedEvent(this, account.getId(), guess, 
                finalAmount, finalCategory != null ? finalCategory.getId() : null));

        log.info("Guess {} accepted with edits by user {}, created transaction {}", 
                guessId, account.getId(), saved.getId());

        return AcceptGuessResponse.builder()
                .guessId(guessId)
                .transactionId(saved.getId())
                .amount(finalAmount)
                .category(finalCategory != null ? finalCategory.getCategoryName() : null)
                .message("Transaction created with your modifications")
                .build();
    }

    /**
     * Reject a spending guess
     */
    @Transactional
    public void rejectGuess(Long guessId, RejectGuessRequest rejectRequest) {
        Account account = SecurityUtils.getCurrentAccount();
        SpendingGuess guess = getGuessForUser(guessId, account.getId());

        validateGuessForAction(guess);

        String reason = rejectRequest != null ? rejectRequest.getReason() : null;
        guess.markRejected(reason);
        guessRepository.save(guess);

        // Publish event for AI learning
        eventPublisher.publishEvent(new GuessRejectedEvent(this, account.getId(), guess, reason));

        log.info("Guess {} rejected by user {} with reason: {}", 
                guessId, account.getId(), reason);
    }

    /**
     * Generate new guesses for a user (called by AI engine or scheduler)
     */
    @Transactional
    public SpendingGuess generateGuess(Long userId) {
        // Check daily limit
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        List<SpendingGuess> todayGuesses = guessRepository.findTodayGuesses(userId, startOfDay, endOfDay);
        
        if (todayGuesses.size() >= MAX_GUESSES_PER_DAY) {
            log.info("User {} has reached daily guess limit of {}", userId, MAX_GUESSES_PER_DAY);
            return null;
        }

        // Generate prediction from AI engine
        Map<String, Object> prediction = predictionEngine.generateDailyPrediction(userId);
        
        if (prediction == null || prediction.isEmpty()) {
            return null;
        }

        // Get default wallet
        Account account = accountRepository.findById(userId).orElse(null);
        if (account == null) return null;

        List<Wallet> wallets = walletRepository.findByAccount(account);
        Wallet defaultWallet = wallets.isEmpty() ? null : wallets.get(0);

        // Get predicted category
        String categoryName = (String) prediction.getOrDefault("category", "General");
        Category category = categoryRepository.findByCategoryName(categoryName).orElse(null);

        // Build guess
        BigDecimal amount = new BigDecimal(String.valueOf(prediction.getOrDefault("predictedAmount", "0")));
        Double confidence = (Double) prediction.getOrDefault("confidence", 0.5);
        String reasoning = (String) prediction.getOrDefault("reasoning", "Based on your spending patterns");

        SpendingGuess guess = SpendingGuess.builder()
                .userId(userId)
                .guessedAmount(amount)
                .currency("VND")
                .category(category)
                .wallet(defaultWallet)
                .confidence(BigDecimal.valueOf(confidence))
                .guessedForTime(LocalDateTime.now())
                .status("PENDING")
                .reasoning(reasoning)
                .expiresAt(LocalDateTime.now().plusHours(GUESS_EXPIRY_HOURS))
                .build();

        return guessRepository.save(guess);
    }

    /**
     * Scheduled job to expire old guesses
     */
    @Scheduled(cron = "0 */30 * * * ?") // Every 30 minutes
    @Transactional
    public void expireOldGuesses() {
        int expired = guessRepository.expireOldGuesses(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} old spending guesses", expired);
        }
    }

    private SpendingGuess getGuessForUser(Long guessId, Long userId) {
        SpendingGuess guess = guessRepository.findById(guessId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "GUESS_NOT_FOUND", "Spending guess not found"));

        if (!guess.getUserId().equals(userId)) {
            throw new CustomException(HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED", "You cannot access this guess");
        }

        return guess;
    }

    private void validateGuessForAction(SpendingGuess guess) {
        if (!guess.isPending()) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "GUESS_ALREADY_PROCESSED", "This guess has already been processed");
        }

        if (guess.isExpired()) {
            guess.markExpired();
            guessRepository.save(guess);
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "GUESS_EXPIRED", "This guess has expired");
        }
    }

    private Transaction createTransactionFromGuess(Account account, SpendingGuess guess) {
        return Transaction.builder()
                .account(account)
                .wallet(guess.getWallet())
                .category(guess.getCategory())
                .amount(guess.getGuessedAmount())
                .description("AI Guess - " + (guess.getCategory() != null ? 
                        guess.getCategory().getCategoryName() : "General"))
                .transactionDate(java.sql.Date.valueOf(LocalDate.now()))
                .build();
    }

    private void updateWalletBalance(Wallet wallet, BigDecimal amount, Category category) {
        if (wallet == null) return;

        BigDecimal newBalance = wallet.getBalance();
        if (category != null) {
            if (category.getCategoryType() == Category.CategoryType.INCOME) {
                newBalance = newBalance.add(amount);
            } else {
                newBalance = newBalance.subtract(amount);
            }
        } else {
            // Default to expense
            newBalance = newBalance.subtract(amount);
        }

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
    }

    private SpendingGuessResponse toResponse(SpendingGuess guess) {
        return SpendingGuessResponse.builder()
                .id(guess.getId())
                .amount(guess.getGuessedAmount())
                .currency(guess.getCurrency())
                .category(guess.getCategory() != null ? guess.getCategory().getCategoryName() : null)
                .categoryId(guess.getCategory() != null ? guess.getCategory().getId() : null)
                .walletName(guess.getWallet() != null ? guess.getWallet().getWalletName() : null)
                .walletId(guess.getWallet() != null ? guess.getWallet().getId() : null)
                .confidence(guess.getConfidence().doubleValue())
                .reasoning(guess.getReasoning())
                .guessedForTime(guess.getGuessedForTime())
                .expiresAt(guess.getExpiresAt())
                .status(guess.getStatus())
                .build();
    }
}
