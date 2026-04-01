package fingenie.com.fingenie.service;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.entity.Transaction;
import fingenie.com.fingenie.entity.Wallet;
import fingenie.com.fingenie.event.TransactionCreatedEvent;
import fingenie.com.fingenie.event.TransactionDeletedEvent;
import fingenie.com.fingenie.event.TransactionUpdatedEvent;
import fingenie.com.fingenie.repository.CategoryRepository;
import fingenie.com.fingenie.repository.TransactionRepository;
import fingenie.com.fingenie.repository.WalletRepository;
import fingenie.com.fingenie.dto.TransactionRequest;
import fingenie.com.fingenie.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Account account = SecurityUtils.getCurrentAccount();
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (!wallet.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Not authorized to create transaction for this wallet");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Update wallet balance based on category type
        BigDecimal newBalance = wallet.getBalance();
        if (category.getCategoryType() == Category.CategoryType.INCOME) {
            newBalance = newBalance.add(request.getAmount());
        } else if (category.getCategoryType() == Category.CategoryType.EXPENSE) {
            newBalance = newBalance.subtract(request.getAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
        } else if (category.getCategoryType() == Category.CategoryType.SAVING) {
            newBalance = newBalance.subtract(request.getAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
        }

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .account(account)
                .wallet(wallet)
                .category(category)
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .build();

        transaction = transactionRepository.save(transaction);
        
        // Publish event for Pet mood updates and AI learning
        // Extract all needed data while still in transaction to avoid lazy loading issues
        eventPublisher.publishEvent(new TransactionCreatedEvent(
                this,
                transaction.getId(),
                account.getId(),
                wallet.getId(),
                category.getId(),
                category.getCategoryName(),
                category.getCategoryType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate()
        ));
        
        return mapToResponse(transaction);
    }

    /**
     * Get transaction by ID with authorization check.
     * OSIV-SAFE: Uses EntityGraph query, maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long transactionId) {
        Account account = SecurityUtils.getCurrentAccount();
        Transaction transaction = transactionRepository.findByIdWithRelations(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Not authorized to access this transaction");
        }

        return mapToResponse(transaction);
    }

    /**
     * Get all transactions for current user (paginated).
     * OSIV-SAFE: Uses EntityGraph query, maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(Pageable pageable) {
        Account account = SecurityUtils.getCurrentAccount();
        return transactionRepository.findByAccount(account, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get all transactions for current user (unpaginated, for internal use).
     * OSIV-SAFE: Uses EntityGraph query, maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        Account account = SecurityUtils.getCurrentAccount();
        return transactionRepository.findByAccount(account).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all transactions for a specific wallet.
     * OSIV-SAFE: Uses EntityGraph query, maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getByWallet(Long walletId) {
        Account account = SecurityUtils.getCurrentAccount();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (!wallet.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Not authorized to access this wallet");
        }

        return transactionRepository.findByWallet(wallet).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse update(Long transactionId, TransactionRequest request) {
        Account account = SecurityUtils.getCurrentAccount();
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Not authorized to update this transaction");
        }

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Revert old transaction impact
        BigDecimal oldAmount = transaction.getAmount();
        Category oldCategory = transaction.getCategory();
        if (oldCategory.getCategoryType() == Category.CategoryType.INCOME) {
            wallet.setBalance(wallet.getBalance().subtract(oldAmount));
        } else {
            wallet.setBalance(wallet.getBalance().add(oldAmount));
        }

        // Apply new transaction impact
        BigDecimal newBalance = wallet.getBalance();
        if (category.getCategoryType() == Category.CategoryType.INCOME) {
            newBalance = newBalance.add(request.getAmount());
        } else {
            newBalance = newBalance.subtract(request.getAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
        }

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        transaction.setWallet(wallet);
        transaction.setCategory(category);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());

        transaction = transactionRepository.save(transaction);

        eventPublisher.publishEvent(new TransactionUpdatedEvent(
                this,
                transaction.getId(),
                account.getId()
        ));

        return mapToResponse(transaction);
    }

    @Transactional
    public void delete(Long transactionId) {
        Account account = SecurityUtils.getCurrentAccount();
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException("Not authorized to delete this transaction");
        }

        // Revert transaction impact on wallet balance
        Wallet wallet = transaction.getWallet();
        Category category = transaction.getCategory();
        BigDecimal amount = transaction.getAmount();

        if (category.getCategoryType() == Category.CategoryType.INCOME) {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        } else {
            wallet.setBalance(wallet.getBalance().add(amount));
        }

        walletRepository.save(wallet);
        Long deletedTransactionId = transaction.getId();
        Long accountId = account.getId();
        transactionRepository.delete(transaction);

        eventPublisher.publishEvent(new TransactionDeletedEvent(
                this,
                deletedTransactionId,
                accountId
        ));
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        Category.CategoryType categoryType = transaction.getCategory().getCategoryType();
        // Map category type to transaction type: SAVING behaves like EXPENSE
        String transactionType = categoryType == Category.CategoryType.INCOME ? "INCOME" : "EXPENSE";
        
        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .walletId(transaction.getWallet().getId())
                .walletName(transaction.getWallet().getWalletName())
                .categoryId(transaction.getCategory().getId())
                .categoryName(transaction.getCategory().getCategoryName())
                .categoryType(categoryType.name())
                .amount(transaction.getAmount())
                .transactionType(transactionType)
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
