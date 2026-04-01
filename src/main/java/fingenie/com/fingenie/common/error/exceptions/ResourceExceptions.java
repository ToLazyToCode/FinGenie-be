package fingenie.com.fingenie.common.error.exceptions;

import fingenie.com.fingenie.common.error.BaseAppException;
import fingenie.com.fingenie.common.error.ErrorCode;

import java.util.Map;

/**
 * Resource Exceptions (Not Found, Conflict, etc.)
 */
public class ResourceExceptions {

    private ResourceExceptions() {
        // Utility class
    }

    // ============================================
    // USER EXCEPTIONS
    // ============================================

    public static class UserNotFoundException extends BaseAppException {
        public UserNotFoundException(Long userId) {
            super(ErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
        }

        public UserNotFoundException(String email) {
            super(ErrorCode.USER_NOT_FOUND, Map.of("email", email));
        }
    }

    public static class EmailAlreadyExistsException extends BaseAppException {
        public EmailAlreadyExistsException(String email) {
            super(ErrorCode.USER_EMAIL_ALREADY_EXISTS, Map.of("email", email));
        }
    }

    public static class PhoneAlreadyExistsException extends BaseAppException {
        public PhoneAlreadyExistsException(String phone) {
            super(ErrorCode.USER_PHONE_ALREADY_EXISTS, Map.of("phone", phone));
        }
    }

    // ============================================
    // WALLET EXCEPTIONS
    // ============================================

    public static class WalletNotFoundException extends BaseAppException {
        public WalletNotFoundException(Long walletId) {
            super(ErrorCode.WALLET_NOT_FOUND, Map.of("walletId", walletId));
        }
    }

    public static class InsufficientBalanceException extends BaseAppException {
        public InsufficientBalanceException(Long walletId, Double required, Double available) {
            super(ErrorCode.WALLET_INSUFFICIENT_BALANCE, Map.of(
                "walletId", walletId,
                "required", required,
                "available", available
            ));
        }
    }

    public static class WalletAccessDeniedException extends BaseAppException {
        public WalletAccessDeniedException(Long walletId) {
            super(ErrorCode.WALLET_ACCESS_DENIED, Map.of("walletId", walletId));
        }
    }

    // ============================================
    // TRANSACTION EXCEPTIONS
    // ============================================

    public static class TransactionNotFoundException extends BaseAppException {
        public TransactionNotFoundException(Long transactionId) {
            super(ErrorCode.TRANSACTION_NOT_FOUND, Map.of("transactionId", transactionId));
        }
    }

    public static class InvalidTransactionAmountException extends BaseAppException {
        public InvalidTransactionAmountException(Double amount) {
            super(ErrorCode.TRANSACTION_INVALID_AMOUNT, Map.of("amount", amount));
        }
    }

    public static class DuplicateTransactionException extends BaseAppException {
        public DuplicateTransactionException(String idempotencyKey) {
            super(ErrorCode.TRANSACTION_DUPLICATE, Map.of("idempotencyKey", idempotencyKey));
        }
    }

    // ============================================
    // BUDGET EXCEPTIONS
    // ============================================

    public static class BudgetNotFoundException extends BaseAppException {
        public BudgetNotFoundException(Long budgetId) {
            super(ErrorCode.BUDGET_NOT_FOUND, Map.of("budgetId", budgetId));
        }
    }

    public static class BudgetExceededException extends BaseAppException {
        public BudgetExceededException(Long budgetId, Double limit, Double current) {
            super(ErrorCode.BUDGET_EXCEEDED, Map.of(
                "budgetId", budgetId,
                "limit", limit,
                "current", current
            ));
        }
    }

    // ============================================
    // PIGGY GOAL EXCEPTIONS
    // ============================================

    public static class PiggyGoalNotFoundException extends BaseAppException {
        public PiggyGoalNotFoundException(Long piggyId) {
            super(ErrorCode.PIGGY_GOAL_NOT_FOUND, Map.of("piggyId", piggyId));
        }
    }

    public static class PiggyGoalCompletedException extends BaseAppException {
        public PiggyGoalCompletedException(Long piggyId) {
            super(ErrorCode.PIGGY_GOAL_COMPLETED, Map.of("piggyId", piggyId));
        }
    }

    // ============================================
    // PET EXCEPTIONS
    // ============================================

    public static class PetNotFoundException extends BaseAppException {
        public PetNotFoundException(Long petId) {
            super(ErrorCode.PET_NOT_FOUND, Map.of("petId", petId));
        }
    }

    public static class PetAlreadyExistsException extends BaseAppException {
        public PetAlreadyExistsException(Long userId) {
            super(ErrorCode.PET_ALREADY_EXISTS, Map.of("userId", userId));
        }
    }

    // ============================================
    // NOTIFICATION EXCEPTIONS
    // ============================================

    public static class NotificationNotFoundException extends BaseAppException {
        public NotificationNotFoundException(Long notificationId) {
            super(ErrorCode.NOTIFICATION_NOT_FOUND, Map.of("notificationId", notificationId));
        }
    }
}
