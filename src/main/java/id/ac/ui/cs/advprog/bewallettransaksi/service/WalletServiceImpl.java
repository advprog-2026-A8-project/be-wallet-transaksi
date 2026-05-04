package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.InvalidAmountException;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.service.strategy.WalletMutationStrategy;
import id.ac.ui.cs.advprog.bewallettransaksi.service.strategy.WalletMutationStrategyResolver;

@Service
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal MINIMUM_AMOUNT = BigDecimal.ONE;
    private static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("99999999999999999.99");
    private static final int MAX_AMOUNT_SCALE = 2;
    private static final String MINIMUM_AMOUNT_MESSAGE = "Amount must be at least 1";
    private static final String MAXIMUM_AMOUNT_MESSAGE = "Amount exceeds maximum allowed value";
    private static final String MAX_SCALE_MESSAGE = "Amount must have at most 2 decimal places";
    private static final String DESCRIPTION_REQUIRED_MESSAGE = "Description must not be blank";
    private static final String USER_ID_REQUIRED_MESSAGE = "User ID must not be null";
    private static final String TOP_UP_REQUEST_REQUIRED_MESSAGE = "Top-up request must not be null";
    private static final String STATUS_REQUIRED_MESSAGE = "Status must not be null";
    private static final String WALLET_ALREADY_EXISTS_MESSAGE = "Wallet already exists for user";
    private static final String PENDING_PAYMENT_NOT_FOUND_MESSAGE = "Pending payment transaction not found for orderId: ";
    private static final String PENDING_TOPUP_NOT_FOUND_MESSAGE = "Pending topup transaction not found for orderId: ";
    private static final String WALLET_NOT_FOUND_FOR_TOPUP_CALLBACK_MESSAGE = "Wallet not found for topup callback";
    private static final String TOPUP_ORDER_PREFIX = "TOPUP-";
    private static final Comparator<Transaction> TRANSACTION_CREATED_AT_ORDER =
            Comparator.comparing(
                    Transaction::getCreatedAt,
                    Comparator.nullsFirst(LocalDateTime::compareTo)
            );
    private static final Comparator<Transaction> TRANSACTION_ID_ORDER =
            Comparator.comparing(
                    Transaction::getTransactionId,
                    Comparator.nullsFirst(UUID::compareTo)
            );
    private static final Comparator<Transaction> TRANSACTION_CREATED_AT_NEWEST =
            TRANSACTION_CREATED_AT_ORDER.thenComparing(TRANSACTION_ID_ORDER);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletMutationStrategyResolver strategyResolver;
    private final OrderPaymentStatusPublisher orderPaymentStatusPublisher;

    public WalletServiceImpl(WalletRepository walletRepository,
                             TransactionRepository transactionRepository,
                             WalletMutationStrategyResolver strategyResolver,
                             OrderPaymentStatusPublisher orderPaymentStatusPublisher) {
        this.walletRepository = requireNonNullDependency(walletRepository, "WalletRepository");
        this.transactionRepository = requireNonNullDependency(transactionRepository, "TransactionRepository");
        this.strategyResolver = requireNonNullDependency(strategyResolver, "WalletMutationStrategyResolver");
        this.orderPaymentStatusPublisher = requireNonNullDependency(
                orderPaymentStatusPublisher,
                "OrderPaymentStatusPublisher"
        );
    }

    private <T> T requireNonNullDependency(T dependency, String dependencyName) {
        return Objects.requireNonNull(dependency, dependencyName + " must not be null");
    }

    @Override
    public WalletResponse getWallet(UUID userId) {
        validateUserId(userId);
        return toResponse(findWalletByUserIdOrThrow(userId));
    }

    @Override
    @Transactional
    public WalletResponse createWallet(UUID userId) {
        validateUserId(userId);
        validateWalletNotExists(userId);
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        Wallet saved = walletRepository.save(wallet);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public WalletResponse topUp(TopUpRequest request) {
        validateTopUpRequest(request);
        validateUserId(request.getUserId());
        validateAmount(request.getAmount());
        Wallet wallet = findWalletByUserIdForUpdateOrThrow(request.getUserId());
        processMutation(
                wallet,
                request.getAmount(),
                TransactionType.TOPUP,
                "Top-up saldo"
        );
        return toResponse(wallet);
    }

    @Override
    @Transactional(noRollbackFor = IllegalStateException.class)
    public WalletResponse pay(UUID userId, BigDecimal amount, String description) {
        validateMutationInput(userId, amount, description);
        Wallet wallet = findWalletByUserIdForUpdateOrThrow(userId);
        validateSufficientBalanceOrRecordFailure(wallet, amount, TransactionType.PAYMENT, description);
        processMutation(
                wallet,
                amount,
                TransactionType.PAYMENT,
                description
        );
        return toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse refund(UUID userId, BigDecimal amount, String description) {
        validateMutationInput(userId, amount, description);
        Wallet wallet = findWalletByUserIdForUpdateOrThrow(userId);
        processMutation(
                wallet,
                amount,
                TransactionType.REFUND,
                description
        );
        return toResponse(wallet);
    }

    @Override
    @Transactional(noRollbackFor = IllegalStateException.class)
    public WalletResponse withdraw(UUID userId, BigDecimal amount, String description) {
        validateMutationInput(userId, amount, description);
        Wallet wallet = findWalletByUserIdForUpdateOrThrow(userId);
        validateSufficientBalanceOrRecordFailure(wallet, amount, TransactionType.WITHDRAW, description);
        processMutation(
                wallet,
                amount,
                TransactionType.WITHDRAW,
                description
        );
        return toResponse(wallet);
    }

    @Override
    public List<TransactionResponse> getTransactionHistory(UUID userId) {
        validateUserId(userId);
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        List<Transaction> transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId());
        return toTransactionResponses(transactions);
    }

    @Override
    public List<TransactionResponse> getTransactionHistoryByStatus(UUID userId, TransactionStatus status) {
        validateUserId(userId);
        validateRequired(status, STATUS_REQUIRED_MESSAGE);
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        List<Transaction> transactions = transactionRepository.findByWalletIdAndStatusOrderByCreatedAtDesc(
                wallet.getWalletId(), status
        );
        return toTransactionResponses(transactions);
    }

    @Override
    @Transactional
    public void handlePaymentSettlement(String orderId) {
        transitionPaymentCallbackStatus(
                orderId,
                TransactionStatus.SUCCESS,
                "Cannot mark payment as settled from status: "
        );
    }

    @Override
    @Transactional
    public void handlePaymentFailure(String orderId) {
        transitionPaymentCallbackStatus(
                orderId,
                TransactionStatus.FAILED,
                "Cannot mark payment as failed from status: "
        );
    }

    private String normalizeOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID must not be blank");
        }
        return orderId.trim();
    }

    private boolean isPendingPaymentTransaction(Transaction transaction) {
        return transaction.getType() == TransactionType.PAYMENT
                && transaction.getStatus() == TransactionStatus.PENDING;
    }

    private Transaction findPendingPaymentByOrderId(String orderId) {
        return findPaymentByOrderId(orderId)
                .filter(this::isPendingPaymentTransaction)
                .orElseThrow(() -> new IllegalStateException(PENDING_PAYMENT_NOT_FOUND_MESSAGE + orderId));
    }

    private java.util.Optional<Transaction> findPaymentByOrderId(String orderId) {
        List<Transaction> matchingPayments = findMatchingPaymentTransactions(orderId);
        java.util.Optional<Transaction> newestPending = findPendingPayment(matchingPayments);
        java.util.Optional<Transaction> latestNonPending = findLatestNonPendingPaymentByCreatedAt(matchingPayments);

        if (shouldPreferLatestNonPending(newestPending, latestNonPending)) {
            return latestNonPending;
        }
        return newestPending.or(() -> findLatestPaymentByCreatedAt(matchingPayments));
    }

    private java.util.Optional<Transaction> findTopUpByOrderId(String orderId) {
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .max(TRANSACTION_CREATED_AT_NEWEST);
    }

    private List<Transaction> findMatchingPaymentTransactions(String orderId) {
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
    }

    private java.util.Optional<Transaction> findPendingPayment(List<Transaction> matchingPayments) {
        return matchingPayments.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .max(TRANSACTION_CREATED_AT_NEWEST);
    }

    private java.util.Optional<Transaction> findLatestPaymentByCreatedAt(List<Transaction> matchingPayments) {
        return matchingPayments.stream().max(TRANSACTION_CREATED_AT_NEWEST);
    }

    private java.util.Optional<Transaction> findLatestNonPendingPaymentByCreatedAt(List<Transaction> matchingPayments) {
        return matchingPayments.stream()
                .filter(transaction -> transaction.getStatus() != TransactionStatus.PENDING)
                .max(TRANSACTION_CREATED_AT_NEWEST);
    }

    private boolean shouldPreferLatestNonPending(
            java.util.Optional<Transaction> newestPending,
            java.util.Optional<Transaction> latestNonPending
    ) {
        if (newestPending.isEmpty() || latestNonPending.isEmpty()) {
            return false;
        }
        return TRANSACTION_CREATED_AT_NEWEST.compare(latestNonPending.get(), newestPending.get()) > 0;
    }

    private void transitionPaymentCallbackStatus(
            String orderId,
            TransactionStatus targetStatus,
            String invalidTransitionPrefix
    ) {
        String normalizedOrderId = normalizeOrderId(orderId);
        Transaction callbackTransaction = findCallbackTransactionByOrderId(normalizedOrderId)
                .orElseThrow(() -> new IllegalStateException(PENDING_PAYMENT_NOT_FOUND_MESSAGE + normalizedOrderId));

        if (callbackTransaction.getStatus() == targetStatus) {
            return;
        }
        if (isOutOfOrderTopUpTerminalTransition(callbackTransaction, targetStatus)) {
            return;
        }
        if (callbackTransaction.getStatus() == TransactionStatus.PENDING) {
            applyPendingCallbackTransition(callbackTransaction, targetStatus, normalizedOrderId);
            return;
        }
        throw new IllegalStateException(invalidTransitionPrefix + callbackTransaction.getStatus());
    }

    private java.util.Optional<Transaction> findCallbackTransactionByOrderId(String normalizedOrderId) {
        java.util.Optional<Transaction> pendingTopUp = findPendingTopUpByOrderId(normalizedOrderId);
        if (isTopUpOrderId(normalizedOrderId) && pendingTopUp.isPresent()) {
            return pendingTopUp;
        }
        return findPaymentByOrderId(normalizedOrderId)
                .or(() -> findTopUpByOrderId(normalizedOrderId));
    }

    private boolean isTopUpOrderId(String orderId) {
        return orderId != null && orderId.startsWith(TOPUP_ORDER_PREFIX);
    }

    private java.util.Optional<Transaction> findPendingTopUpByOrderId(String normalizedOrderId) {
        return findTopUpByOrderId(normalizedOrderId)
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING);
    }

    private void applyPendingCallbackTransition(
            Transaction callbackTransaction,
            TransactionStatus targetStatus,
            String normalizedOrderId
    ) {
        if (callbackTransaction.getType() == TransactionType.TOPUP) {
            transitionPendingTopUp(callbackTransaction, targetStatus, normalizedOrderId);
            return;
        }
        updateTransactionStatus(callbackTransaction, targetStatus);
        publishOrderPaymentStatusUpdate(normalizedOrderId, targetStatus);
    }

    private boolean isOutOfOrderTopUpTerminalTransition(
            Transaction callbackTransaction,
            TransactionStatus targetStatus
    ) {
        if (callbackTransaction.getType() != TransactionType.TOPUP) {
            return false;
        }
        return (callbackTransaction.getStatus() == TransactionStatus.SUCCESS && targetStatus == TransactionStatus.FAILED)
                || (callbackTransaction.getStatus() == TransactionStatus.FAILED
                && targetStatus == TransactionStatus.SUCCESS);
    }

    private void transitionPendingTopUp(
            Transaction topUpTransaction,
            TransactionStatus targetStatus,
            String normalizedOrderId
    ) {
        if (topUpTransaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException(PENDING_TOPUP_NOT_FOUND_MESSAGE + normalizedOrderId);
        }
        if (targetStatus == TransactionStatus.SUCCESS) {
            Wallet wallet = walletRepository.findById(topUpTransaction.getWalletId())
                    .orElseThrow(() -> new IllegalStateException(WALLET_NOT_FOUND_FOR_TOPUP_CALLBACK_MESSAGE));
            wallet.setBalance(wallet.getBalance().add(topUpTransaction.getAmount()));
            walletRepository.save(wallet);
        }
        updateTransactionStatus(topUpTransaction, targetStatus);
    }

    private void publishOrderPaymentStatusUpdate(String orderId, TransactionStatus targetStatus) {
        switch (targetStatus) {
            case SUCCESS -> runSafely(() -> orderPaymentStatusPublisher.publishPaymentSettled(orderId));
            case FAILED -> runSafely(() -> orderPaymentStatusPublisher.publishPaymentFailed(orderId));
            default -> {
                // Ignore non-terminal states for order payment publication.
            }
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            // Keep wallet transaction state authoritative even if downstream publication fails.
        }
    }

    private void updateTransactionStatus(Transaction transaction, TransactionStatus status) {
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }

    private void validateUserId(UUID userId) {
        validateRequired(userId, USER_ID_REQUIRED_MESSAGE);
    }

    private void validateMutationInput(UUID userId, BigDecimal amount, String description) {
        validateUserId(userId);
        validateAmount(amount);
        validateDescription(description);
    }

    private void validateTopUpRequest(TopUpRequest request) {
        validateRequired(request, TOP_UP_REQUEST_REQUIRED_MESSAGE);
    }

    private void validateRequired(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || isBelowMinimumAmount(amount)) {
            throw new InvalidAmountException(MINIMUM_AMOUNT_MESSAGE);
        }
        validateScale(amount);
        validateNotAboveMaximum(amount);
    }

    private boolean isBelowMinimumAmount(BigDecimal amount) {
        return amount.compareTo(MINIMUM_AMOUNT) < 0;
    }

    private boolean hasMoreThanTwoDecimalPlaces(BigDecimal amount) {
        return amount.scale() > MAX_AMOUNT_SCALE;
    }

    private boolean isAboveMaximumAmount(BigDecimal amount) {
        return amount.compareTo(MAXIMUM_AMOUNT) > 0;
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(DESCRIPTION_REQUIRED_MESSAGE);
        }
    }

    private Wallet findWalletByUserIdOrThrow(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
    }

    private Wallet findWalletByUserIdForUpdateOrThrow(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
    }

    private void validateWalletNotExists(UUID userId) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException(WALLET_ALREADY_EXISTS_MESSAGE);
        }
    }

    private void validateSufficientBalanceOrRecordFailure(
            Wallet wallet,
            BigDecimal amount,
            TransactionType type,
            String description
    ) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            recordFailedTransaction(wallet.getWalletId(), amount, type, description);
            throw new IllegalStateException("Insufficient balance");
        }
    }

    private void recordFailedTransaction(UUID walletId, BigDecimal amount, TransactionType type, String description) {
        Transaction failedTransaction = createTransaction(walletId, amount, type, description);
        failedTransaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(failedTransaction);
    }

    private void processMutation(Wallet wallet, BigDecimal amount, TransactionType type,
                                 String description) {
        BigDecimal updatedBalance = applyMutation(wallet.getBalance(), amount, type);
        validateUpdatedBalance(updatedBalance);
        Transaction transaction = createTransaction(wallet.getWalletId(), amount, type, description);
        updateWalletBalance(wallet, updatedBalance);
        finalizeTransaction(transaction);
    }

    private BigDecimal applyMutation(BigDecimal currentBalance, BigDecimal amount, TransactionType type) {
        WalletMutationStrategy strategy = strategyResolver.resolve(type);
        return strategy.apply(currentBalance, amount);
    }

    private void validateUpdatedBalance(BigDecimal updatedBalance) {
        validateNotAboveMaximum(updatedBalance);
    }

    private void validateScale(BigDecimal amount) {
        if (hasMoreThanTwoDecimalPlaces(amount)) {
            throw new InvalidAmountException(MAX_SCALE_MESSAGE);
        }
    }

    private void validateNotAboveMaximum(BigDecimal amount) {
        if (isAboveMaximumAmount(amount)) {
            throw new InvalidAmountException(MAXIMUM_AMOUNT_MESSAGE);
        }
    }

    private void updateWalletBalance(Wallet wallet, BigDecimal updatedBalance) {
        wallet.setBalance(normalizeBalance(updatedBalance));
        walletRepository.save(wallet);
    }

    private void finalizeTransaction(Transaction transaction) {
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
    }

    private BigDecimal normalizeBalance(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return balance;
    }

    private Transaction createTransaction(UUID walletId, BigDecimal amount,
                                          TransactionType type, String description) {
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription(description);
        return transaction;
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .build();
    }

    private List<TransactionResponse> toTransactionResponses(List<Transaction> transactions) {
        return transactions.stream().map(this::toTransactionResponse).toList();
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .walletId(transaction.getWalletId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
