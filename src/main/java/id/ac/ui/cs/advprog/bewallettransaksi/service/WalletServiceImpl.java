package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletMutationStrategyResolver strategyResolver;

    public WalletServiceImpl(WalletRepository walletRepository,
                             TransactionRepository transactionRepository,
                             WalletMutationStrategyResolver strategyResolver) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.strategyResolver = strategyResolver != null
                ? strategyResolver
                : new WalletMutationStrategyResolver();
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
    @Transactional
    public WalletResponse pay(UUID userId, BigDecimal amount, String description) {
        validateMutationInput(userId, amount, description);
        Wallet wallet = findWalletByUserIdForUpdateOrThrow(userId);
        validateSufficientBalance(wallet, amount);
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
    @Transactional
    public WalletResponse withdraw(UUID userId, BigDecimal amount, String description) {
        validateMutationInput(userId, amount, description);
        Wallet wallet = findWalletByUserIdForUpdateOrThrow(userId);
        validateSufficientBalance(wallet, amount);
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
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        List<Transaction> transactions = transactionRepository.findByWalletIdAndStatusOrderByCreatedAtDesc(
                wallet.getWalletId(), status
        );
        return toTransactionResponses(transactions);
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

    private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
    }

    private void processMutation(Wallet wallet, BigDecimal amount, TransactionType type,
                                 String description) {
        WalletMutationStrategy strategy = strategyResolver.resolve(type);
        BigDecimal updatedBalance = strategy.apply(wallet.getBalance(), amount);
        validateUpdatedBalance(updatedBalance);
        Transaction transaction = createTransaction(wallet.getWalletId(), amount, type, description);
        updateWalletBalance(wallet, updatedBalance);
        finalizeTransaction(transaction);
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
        return transactions.stream().map(this::toTransactionResponse).collect(Collectors.toList());
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
