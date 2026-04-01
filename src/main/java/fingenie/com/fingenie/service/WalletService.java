package fingenie.com.fingenie.service;

import fingenie.com.fingenie.common.error.exceptions.AuthenticationExceptions;
import fingenie.com.fingenie.common.error.exceptions.ResourceExceptions;
import fingenie.com.fingenie.common.error.exceptions.SystemExceptions;
import fingenie.com.fingenie.config.CreationGuardrailConfig;
import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.entity.Account;
import fingenie.com.fingenie.entity.Wallet;
import fingenie.com.fingenie.repository.WalletRepository;
import fingenie.com.fingenie.dto.WalletRequest;
import fingenie.com.fingenie.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final CreationGuardrailConfig guardrailConfig;
    private final CreationThrottleService creationThrottleService;

    @Transactional
    public WalletResponse create(WalletRequest request) {
        Account account = SecurityUtils.getCurrentAccount();
        Long accountId = account.getId();

        creationThrottleService.assertWalletCreateAllowed(accountId);

        long existingWalletCount = walletRepository.countByAccountId(accountId);
        int maxPerAccount = Math.max(guardrailConfig.getWallet().getMaxPerAccount(), 1);
        if (existingWalletCount >= maxPerAccount) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "walletMaxPerAccount", maxPerAccount,
                    "walletCount", existingWalletCount
            ));
        }

        String walletName = normalizeWalletName(request.getWalletName());
        if (walletRepository.existsByAccountIdAndWalletNameIgnoreCase(accountId, walletName)) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "walletName", "duplicate"
            ));
        }

        // If this is set as default, unset other defaults
        if (request.isDefault()) {
            walletRepository.findByAccountAndIsDefaultTrue(account)
                    .ifPresent(wallet -> {
                        wallet.setDefault(false);
                        walletRepository.save(wallet);
                    });
        }

        Wallet wallet = Wallet.builder()
                .account(account)
                .walletName(walletName)
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .isDefault(request.isDefault())
                .build();

        wallet = walletRepository.save(wallet);
        return mapToResponse(wallet);
    }

    /**
     * Get wallet by ID with authorization check.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public WalletResponse getById(Long walletId) {
        Account account = SecurityUtils.getCurrentAccount();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceExceptions.WalletNotFoundException(walletId));

        if (!wallet.getAccount().getId().equals(account.getId())) {
            throw new ResourceExceptions.WalletAccessDeniedException(walletId);
        }

        return mapToResponse(wallet);
    }

    /**
     * Get all wallets for current user.
     * OSIV-SAFE: Maps to DTOs within transaction.
     */
    @Transactional(readOnly = true)
    public List<WalletResponse> getAll() {
        Account account = SecurityUtils.getCurrentAccount();
        return walletRepository.findByAccount(account).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get default wallet for current user.
     * OSIV-SAFE: Maps to DTO within transaction.
     */
    @Transactional(readOnly = true)
    public WalletResponse getDefault() {
        Account account = SecurityUtils.getCurrentAccount();
        Wallet wallet = walletRepository.findByAccountAndIsDefaultTrue(account)
                .orElseThrow(() -> new ResourceExceptions.WalletNotFoundException(-1L));
        
        return mapToResponse(wallet);
    }

    @Transactional
    public WalletResponse update(Long walletId, WalletRequest request) {
        Account account = SecurityUtils.getCurrentAccount();
        Long accountId = account.getId();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceExceptions.WalletNotFoundException(walletId));

        if (!wallet.getAccount().getId().equals(accountId)) {
            throw new AuthenticationExceptions.AccessDeniedException("wallet");
        }

        // If setting as default, unset other defaults
        if (request.isDefault() && !wallet.isDefault()) {
            walletRepository.findByAccountAndIsDefaultTrue(account)
                    .ifPresent(w -> {
                        w.setDefault(false);
                        walletRepository.save(w);
                    });
        }

        String walletName = normalizeWalletName(request.getWalletName());
        if (walletRepository.existsByAccountIdAndWalletNameIgnoreCaseAndIdNot(accountId, walletName, walletId)) {
            throw new SystemExceptions.ValidationException(Map.of(
                    "walletName", "duplicate"
            ));
        }

        wallet.setWalletName(walletName);
        if (request.getBalance() != null) {
            wallet.setBalance(request.getBalance());
        }
        wallet.setDefault(request.isDefault());

        wallet = walletRepository.save(wallet);
        return mapToResponse(wallet);
    }

    @Transactional
    public void delete(Long walletId) {
        Account account = SecurityUtils.getCurrentAccount();
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceExceptions.WalletNotFoundException(walletId));

        if (!wallet.getAccount().getId().equals(account.getId())) {
            throw new AuthenticationExceptions.AccessDeniedException("wallet");
        }

        walletRepository.delete(wallet);
    }

    private String normalizeWalletName(String value) {
        String normalized = Optional.ofNullable(value).orElse("").trim();
        if (normalized.isEmpty()) {
            throw new SystemExceptions.ValidationException(Map.of("walletName", "required"));
        }
        return normalized;
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .accountId(wallet.getAccount().getId())
                .walletName(wallet.getWalletName())
                .balance(wallet.getBalance())
                .isDefault(wallet.isDefault())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
