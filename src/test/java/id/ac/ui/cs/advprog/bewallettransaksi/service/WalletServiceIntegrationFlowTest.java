package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.InvalidAmountException;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class WalletServiceIntegrationFlowTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void fullMutationFlow_ShouldProduceExpectedBalanceAndHistory() {
        UUID userId = UUID.randomUUID();
        walletService.createWallet(userId);

        TopUpRequest topUpRequest = new TopUpRequest();
        topUpRequest.setUserId(userId);
        topUpRequest.setAmount(new BigDecimal("1000000.00"));
        walletService.topUp(topUpRequest);

        walletService.pay(userId, new BigDecimal("250000.00"), "Order payment");
        walletService.refund(userId, new BigDecimal("50000.00"), "Order refund");
        WalletResponse finalWallet = walletService.withdraw(userId, new BigDecimal("100000.00"), "BCA-123456");

        assertEquals(new BigDecimal("700000.00"), finalWallet.getBalance());

        List<TransactionResponse> history = walletService.getTransactionHistory(userId);
        assertEquals(4, history.size());

        List<TransactionType> types = history.stream()
                .map(TransactionResponse::getType)
                .toList();
        assertEquals(1, types.stream().filter(type -> type == TransactionType.TOPUP).count());
        assertEquals(1, types.stream().filter(type -> type == TransactionType.PAYMENT).count());
        assertEquals(1, types.stream().filter(type -> type == TransactionType.REFUND).count());
        assertEquals(1, types.stream().filter(type -> type == TransactionType.WITHDRAW).count());
        assertEquals(4, history.stream().filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS).count());
    }

    @Test
    void topUp_ShouldRejectWhenResultingBalanceExceedsMaximumBoundary() {
        UUID userId = UUID.randomUUID();
        walletService.createWallet(userId);

        TopUpRequest maxTopUpRequest = new TopUpRequest();
        maxTopUpRequest.setUserId(userId);
        maxTopUpRequest.setAmount(new BigDecimal("99999999999999999.99"));
        walletService.topUp(maxTopUpRequest);

        TopUpRequest overflowRequest = new TopUpRequest();
        overflowRequest.setUserId(userId);
        overflowRequest.setAmount(BigDecimal.ONE);

        assertThrows(InvalidAmountException.class, () -> walletService.topUp(overflowRequest));
    }

    @Test
    void withdraw_InsufficientBalance_ShouldPersistFailedTransaction() {
        UUID userId = UUID.randomUUID();
        walletService.createWallet(userId);

        TopUpRequest topUpRequest = new TopUpRequest();
        topUpRequest.setUserId(userId);
        topUpRequest.setAmount(new BigDecimal("100.00"));
        walletService.topUp(topUpRequest);

        BigDecimal withdrawAmount = new BigDecimal("150.00");
        assertThrows(IllegalStateException.class,
                () -> walletService.withdraw(userId, withdrawAmount, "BCA-123456"));

        WalletResponse wallet = walletService.getWallet(userId);
        assertEquals(new BigDecimal("100.00"), wallet.getBalance());

        List<TransactionResponse> failedHistory = walletService.getTransactionHistoryByStatus(userId, TransactionStatus.FAILED);
        assertEquals(1, failedHistory.size());
        assertEquals(TransactionType.WITHDRAW, failedHistory.get(0).getType());
        assertEquals(new BigDecimal("150.00"), failedHistory.get(0).getAmount());
    }

    @Test
    void pay_ShouldCreateSinglePaymentTransactionRecord() {
        UUID userId = UUID.randomUUID();
        walletService.createWallet(userId);

        TopUpRequest topUpRequest = new TopUpRequest();
        topUpRequest.setUserId(userId);
        topUpRequest.setAmount(new BigDecimal("100.00"));
        walletService.topUp(topUpRequest);

        walletService.pay(userId, new BigDecimal("40.00"), "Order payment");

        List<TransactionResponse> history = walletService.getTransactionHistory(userId);
        long paymentCount = history.stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .count();

        assertEquals(1, paymentCount);
        TransactionResponse paymentTransaction = history.stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.SUCCESS, paymentTransaction.getStatus());
    }

    @Test
    void handlePaymentSettlement_WhenLatestDuplicateIsSuccess_ShouldNotProcessOlderPending() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String orderId = "ORDER-INTEG-1";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction olderPending = new Transaction();
        olderPending.setWalletId(wallet.getWalletId());
        olderPending.setAmount(new BigDecimal("100.00"));
        olderPending.setType(TransactionType.PAYMENT);
        olderPending.setStatus(TransactionStatus.PENDING);
        olderPending.setDescription(orderId);
        olderPending.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        olderPending.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transactionRepository.save(olderPending);
        pauseForDistinctPersistTimestamp();

        Transaction latestSuccess = new Transaction();
        latestSuccess.setWalletId(wallet.getWalletId());
        latestSuccess.setAmount(new BigDecimal("100.00"));
        latestSuccess.setType(TransactionType.PAYMENT);
        latestSuccess.setStatus(TransactionStatus.SUCCESS);
        latestSuccess.setDescription(orderId);
        latestSuccess.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        latestSuccess.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        transactionRepository.save(latestSuccess);

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(orderId));

        List<Transaction> persisted = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
        long pendingCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        long successCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .count();
        assertEquals(1, pendingCount);
        assertEquals(1, successCount);
    }

    @Test
    void handlePaymentSettlement_WhenTopUpAlreadySucceeded_ShouldNotCreditDuplicatePendingTopUp() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-DUPLICATE-SETTLE-001";
        BigDecimal topUpAmount = new BigDecimal("110000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        wallet.setBalance(topUpAmount);
        walletRepository.save(wallet);

        Transaction alreadySuccessTopUp = new Transaction();
        alreadySuccessTopUp.setWalletId(wallet.getWalletId());
        alreadySuccessTopUp.setAmount(topUpAmount);
        alreadySuccessTopUp.setType(TransactionType.TOPUP);
        alreadySuccessTopUp.setStatus(TransactionStatus.SUCCESS);
        alreadySuccessTopUp.setDescription(topUpOrderId);
        alreadySuccessTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 9, 0));
        alreadySuccessTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 9, 0));
        transactionRepository.save(alreadySuccessTopUp);
        pauseForDistinctPersistTimestamp();

        Transaction duplicatePendingTopUp = new Transaction();
        duplicatePendingTopUp.setWalletId(wallet.getWalletId());
        duplicatePendingTopUp.setAmount(topUpAmount);
        duplicatePendingTopUp.setType(TransactionType.TOPUP);
        duplicatePendingTopUp.setStatus(TransactionStatus.PENDING);
        duplicatePendingTopUp.setDescription(topUpOrderId);
        duplicatePendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 9, 5));
        duplicatePendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 9, 5));
        transactionRepository.save(duplicatePendingTopUp);

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(topUpOrderId));

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(topUpAmount, persistedWallet.getBalance());

        long successCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .count();
        long pendingCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        assertEquals(1, successCount);
        assertEquals(1, pendingCount);
    }

    @Test
    void handlePaymentSettlement_WhenTopUpAlreadyFailed_ShouldNotPromoteNewerDuplicatePendingTopUp() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-DUPLICATE-FAILED-SETTLE-001";
        BigDecimal topUpAmount = new BigDecimal("93000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction alreadyFailedTopUp = new Transaction();
        alreadyFailedTopUp.setWalletId(wallet.getWalletId());
        alreadyFailedTopUp.setAmount(topUpAmount);
        alreadyFailedTopUp.setType(TransactionType.TOPUP);
        alreadyFailedTopUp.setStatus(TransactionStatus.FAILED);
        alreadyFailedTopUp.setDescription(topUpOrderId);
        alreadyFailedTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 10, 0));
        alreadyFailedTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 10, 0));
        transactionRepository.save(alreadyFailedTopUp);
        pauseForDistinctPersistTimestamp();

        Transaction duplicatePendingTopUp = new Transaction();
        duplicatePendingTopUp.setWalletId(wallet.getWalletId());
        duplicatePendingTopUp.setAmount(topUpAmount);
        duplicatePendingTopUp.setType(TransactionType.TOPUP);
        duplicatePendingTopUp.setStatus(TransactionStatus.PENDING);
        duplicatePendingTopUp.setDescription(topUpOrderId);
        duplicatePendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 10, 5));
        duplicatePendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 10, 5));
        transactionRepository.save(duplicatePendingTopUp);

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(topUpOrderId));

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        long failedCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.FAILED)
                .count();
        long pendingCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        assertEquals(1, failedCount);
        assertEquals(1, pendingCount);
    }

    @Test
    void handlePaymentFailure_WhenTopUpAlreadySucceeded_ShouldNotDemoteNewerDuplicatePendingTopUp() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-DUPLICATE-SUCCESS-FAIL-001";
        BigDecimal topUpAmount = new BigDecimal("101000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        wallet.setBalance(topUpAmount);
        walletRepository.save(wallet);

        Transaction alreadySucceededTopUp = new Transaction();
        alreadySucceededTopUp.setWalletId(wallet.getWalletId());
        alreadySucceededTopUp.setAmount(topUpAmount);
        alreadySucceededTopUp.setType(TransactionType.TOPUP);
        alreadySucceededTopUp.setStatus(TransactionStatus.SUCCESS);
        alreadySucceededTopUp.setDescription(topUpOrderId);
        alreadySucceededTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 11, 0));
        alreadySucceededTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 11, 0));
        transactionRepository.save(alreadySucceededTopUp);
        pauseForDistinctPersistTimestamp();

        Transaction duplicatePendingTopUp = new Transaction();
        duplicatePendingTopUp.setWalletId(wallet.getWalletId());
        duplicatePendingTopUp.setAmount(topUpAmount);
        duplicatePendingTopUp.setType(TransactionType.TOPUP);
        duplicatePendingTopUp.setStatus(TransactionStatus.PENDING);
        duplicatePendingTopUp.setDescription(topUpOrderId);
        duplicatePendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 11, 5));
        duplicatePendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 11, 5));
        transactionRepository.save(duplicatePendingTopUp);

        assertDoesNotThrow(() -> walletService.handlePaymentFailure(topUpOrderId));

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(topUpAmount, persistedWallet.getBalance());

        long successCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .count();
        long pendingCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        assertEquals(1, successCount);
        assertEquals(1, pendingCount);
    }

    @Test
    void handlePaymentFailure_WhenTopUpAlreadyFailed_ShouldIgnoreDuplicatePendingTopUp() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-DUPLICATE-FAILED-FAIL-001";
        BigDecimal topUpAmount = new BigDecimal("97000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction alreadyFailedTopUp = new Transaction();
        alreadyFailedTopUp.setWalletId(wallet.getWalletId());
        alreadyFailedTopUp.setAmount(topUpAmount);
        alreadyFailedTopUp.setType(TransactionType.TOPUP);
        alreadyFailedTopUp.setStatus(TransactionStatus.FAILED);
        alreadyFailedTopUp.setDescription(topUpOrderId);
        alreadyFailedTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 12, 0));
        alreadyFailedTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 12, 0));
        transactionRepository.save(alreadyFailedTopUp);
        pauseForDistinctPersistTimestamp();

        Transaction duplicatePendingTopUp = new Transaction();
        duplicatePendingTopUp.setWalletId(wallet.getWalletId());
        duplicatePendingTopUp.setAmount(topUpAmount);
        duplicatePendingTopUp.setType(TransactionType.TOPUP);
        duplicatePendingTopUp.setStatus(TransactionStatus.PENDING);
        duplicatePendingTopUp.setDescription(topUpOrderId);
        duplicatePendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 2, 12, 5));
        duplicatePendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 12, 5));
        transactionRepository.save(duplicatePendingTopUp);

        assertDoesNotThrow(() -> walletService.handlePaymentFailure(topUpOrderId));

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        long failedCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.FAILED)
                .count();
        long pendingCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        assertEquals(1, failedCount);
        assertEquals(1, pendingCount);
    }

    @Test
    void handlePaymentFailure_WhenLatestDuplicateIsFailed_ShouldNotProcessOlderPending() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String orderId = "ORDER-INTEG-2";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction olderPending = new Transaction();
        olderPending.setWalletId(wallet.getWalletId());
        olderPending.setAmount(new BigDecimal("100.00"));
        olderPending.setType(TransactionType.PAYMENT);
        olderPending.setStatus(TransactionStatus.PENDING);
        olderPending.setDescription(orderId);
        olderPending.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        olderPending.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transactionRepository.save(olderPending);
        pauseForDistinctPersistTimestamp();

        Transaction latestFailed = new Transaction();
        latestFailed.setWalletId(wallet.getWalletId());
        latestFailed.setAmount(new BigDecimal("100.00"));
        latestFailed.setType(TransactionType.PAYMENT);
        latestFailed.setStatus(TransactionStatus.FAILED);
        latestFailed.setDescription(orderId);
        latestFailed.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        latestFailed.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        transactionRepository.save(latestFailed);

        assertDoesNotThrow(() -> walletService.handlePaymentFailure(orderId));

        List<Transaction> persisted = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
        long pendingCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        long failedCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.FAILED)
                .count();
        assertEquals(1, pendingCount);
        assertEquals(1, failedCount);
    }

    @Test
    void handlePaymentFailure_WhenLatestDuplicateIsSuccess_ShouldNotDemoteOlderPendingPayment() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String orderId = "ORDER-INTEG-3";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction olderPending = new Transaction();
        olderPending.setWalletId(wallet.getWalletId());
        olderPending.setAmount(new BigDecimal("100.00"));
        olderPending.setType(TransactionType.PAYMENT);
        olderPending.setStatus(TransactionStatus.PENDING);
        olderPending.setDescription(orderId);
        olderPending.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        olderPending.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transactionRepository.save(olderPending);
        pauseForDistinctPersistTimestamp();

        Transaction latestSuccess = new Transaction();
        latestSuccess.setWalletId(wallet.getWalletId());
        latestSuccess.setAmount(new BigDecimal("100.00"));
        latestSuccess.setType(TransactionType.PAYMENT);
        latestSuccess.setStatus(TransactionStatus.SUCCESS);
        latestSuccess.setDescription(orderId);
        latestSuccess.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        latestSuccess.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        transactionRepository.save(latestSuccess);

        assertDoesNotThrow(() -> walletService.handlePaymentFailure(orderId));

        List<Transaction> persisted = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
        long pendingCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        long successCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .count();
        assertEquals(1, pendingCount);
        assertEquals(1, successCount);
    }

    @Test
    void handlePaymentSettlement_WhenLatestDuplicateIsFailed_ShouldNotPromoteOlderPendingPayment() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String orderId = "ORDER-INTEG-4";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction olderPending = new Transaction();
        olderPending.setWalletId(wallet.getWalletId());
        olderPending.setAmount(new BigDecimal("100.00"));
        olderPending.setType(TransactionType.PAYMENT);
        olderPending.setStatus(TransactionStatus.PENDING);
        olderPending.setDescription(orderId);
        olderPending.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        olderPending.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transactionRepository.save(olderPending);
        pauseForDistinctPersistTimestamp();

        Transaction latestFailed = new Transaction();
        latestFailed.setWalletId(wallet.getWalletId());
        latestFailed.setAmount(new BigDecimal("100.00"));
        latestFailed.setType(TransactionType.PAYMENT);
        latestFailed.setStatus(TransactionStatus.FAILED);
        latestFailed.setDescription(orderId);
        latestFailed.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        latestFailed.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        transactionRepository.save(latestFailed);

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(orderId));

        List<Transaction> persisted = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
        long pendingCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        long failedCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.FAILED)
                .count();
        assertEquals(1, pendingCount);
        assertEquals(1, failedCount);
    }

    @Test
    void handlePaymentSettlement_WhenLatestDuplicateIsFailed_ShouldRemainNoOpOnRepeatedSettlement() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String orderId = "ORDER-INTEG-5";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction olderPending = new Transaction();
        olderPending.setWalletId(wallet.getWalletId());
        olderPending.setAmount(new BigDecimal("100.00"));
        olderPending.setType(TransactionType.PAYMENT);
        olderPending.setStatus(TransactionStatus.PENDING);
        olderPending.setDescription(orderId);
        olderPending.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        olderPending.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transactionRepository.save(olderPending);
        pauseForDistinctPersistTimestamp();

        Transaction latestFailed = new Transaction();
        latestFailed.setWalletId(wallet.getWalletId());
        latestFailed.setAmount(new BigDecimal("100.00"));
        latestFailed.setType(TransactionType.PAYMENT);
        latestFailed.setStatus(TransactionStatus.FAILED);
        latestFailed.setDescription(orderId);
        latestFailed.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        latestFailed.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        transactionRepository.save(latestFailed);

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(orderId));
        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(orderId));

        List<Transaction> persisted = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
        long pendingCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        long failedCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.FAILED)
                .count();
        assertEquals(1, pendingCount);
        assertEquals(1, failedCount);
    }

    @Test
    void handlePaymentFailure_WhenLatestDuplicateIsSuccess_ShouldRemainNoOpOnRepeatedFailure() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String orderId = "ORDER-INTEG-6";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction olderPending = new Transaction();
        olderPending.setWalletId(wallet.getWalletId());
        olderPending.setAmount(new BigDecimal("100.00"));
        olderPending.setType(TransactionType.PAYMENT);
        olderPending.setStatus(TransactionStatus.PENDING);
        olderPending.setDescription(orderId);
        olderPending.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        olderPending.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transactionRepository.save(olderPending);
        pauseForDistinctPersistTimestamp();

        Transaction latestSuccess = new Transaction();
        latestSuccess.setWalletId(wallet.getWalletId());
        latestSuccess.setAmount(new BigDecimal("100.00"));
        latestSuccess.setType(TransactionType.PAYMENT);
        latestSuccess.setStatus(TransactionStatus.SUCCESS);
        latestSuccess.setDescription(orderId);
        latestSuccess.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        latestSuccess.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        transactionRepository.save(latestSuccess);

        assertDoesNotThrow(() -> walletService.handlePaymentFailure(orderId));
        assertDoesNotThrow(() -> walletService.handlePaymentFailure(orderId));

        List<Transaction> persisted = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
        long pendingCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        long successCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .count();
        assertEquals(1, pendingCount);
        assertEquals(1, successCount);
    }

    @Test
    void handlePaymentSettlement_WhenLatestDuplicateIsSuccess_ShouldRemainNoOpOnRepeatedSettlement() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String orderId = "ORDER-INTEG-7";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction olderPending = new Transaction();
        olderPending.setWalletId(wallet.getWalletId());
        olderPending.setAmount(new BigDecimal("100.00"));
        olderPending.setType(TransactionType.PAYMENT);
        olderPending.setStatus(TransactionStatus.PENDING);
        olderPending.setDescription(orderId);
        olderPending.setCreatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        olderPending.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        transactionRepository.save(olderPending);
        pauseForDistinctPersistTimestamp();

        Transaction latestSuccess = new Transaction();
        latestSuccess.setWalletId(wallet.getWalletId());
        latestSuccess.setAmount(new BigDecimal("100.00"));
        latestSuccess.setType(TransactionType.PAYMENT);
        latestSuccess.setStatus(TransactionStatus.SUCCESS);
        latestSuccess.setDescription(orderId);
        latestSuccess.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        latestSuccess.setUpdatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        transactionRepository.save(latestSuccess);

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(orderId));
        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(orderId));

        List<Transaction> persisted = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equals(transaction.getDescription()))
                .toList();
        long pendingCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .count();
        long successCount = persisted.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .count();
        assertEquals(1, pendingCount);
        assertEquals(1, successCount);
    }

    @Test
    void handlePaymentSettlement_UnknownOrderId_ShouldThrowPaymentNotFoundMessage() {
        String unknownOrderId = "ORDER-UNKNOWN-001";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> walletService.handlePaymentSettlement(unknownOrderId)
        );
        assertEquals("Payment transaction not found for orderId: " + unknownOrderId, exception.getMessage());
    }

    @Test
    void handlePaymentFailure_UnknownOrderId_ShouldThrowPaymentNotFoundMessage() {
        String unknownOrderId = "ORDER-UNKNOWN-002";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> walletService.handlePaymentFailure(unknownOrderId)
        );
        assertEquals("Payment transaction not found for orderId: " + unknownOrderId, exception.getMessage());
    }

    @Test
    void handlePaymentSettlement_UnknownTopUpOrderId_ShouldThrowTopUpNotFoundMessage() {
        String unknownTopUpOrderId = "TOPUP-UNKNOWN-001";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> walletService.handlePaymentSettlement(unknownTopUpOrderId)
        );
        assertEquals("Topup transaction not found for orderId: " + unknownTopUpOrderId, exception.getMessage());
    }

    @Test
    void handlePaymentFailure_UnknownTopUpOrderId_ShouldThrowTopUpNotFoundMessage() {
        String unknownTopUpOrderId = "TOPUP-UNKNOWN-002";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> walletService.handlePaymentFailure(unknownTopUpOrderId)
        );
        assertEquals("Topup transaction not found for orderId: " + unknownTopUpOrderId, exception.getMessage());
    }

    @Test
    void handlePaymentFailure_AfterTopUpSuccess_ShouldRemainNoOpOnRepeatedFailure() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-REPEAT-FAILURE-001";
        BigDecimal topUpAmount = new BigDecimal("88000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(topUpOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 3, 10, 0));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 3, 10, 0));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentSettlement(topUpOrderId);
        assertDoesNotThrow(() -> walletService.handlePaymentFailure(topUpOrderId));
        assertDoesNotThrow(() -> walletService.handlePaymentFailure(topUpOrderId));

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(topUpAmount, persistedWallet.getBalance());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.SUCCESS, persistedTopUp.getStatus());
    }

    @Test
    void handlePaymentSettlement_BlankOrderId_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.handlePaymentSettlement("   ")
        );
        assertTrue(exception.getMessage().contains("Order ID must not be blank"));
    }

    @Test
    void handlePaymentFailure_BlankOrderId_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.handlePaymentFailure("   ")
        );
        assertTrue(exception.getMessage().contains("Order ID must not be blank"));
    }

    @Test
    void handlePaymentSettlement_NullOrderId_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.handlePaymentSettlement(null)
        );
        assertTrue(exception.getMessage().contains("Order ID must not be blank"));
    }

    @Test
    void handlePaymentFailure_NullOrderId_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.handlePaymentFailure(null)
        );
        assertTrue(exception.getMessage().contains("Order ID must not be blank"));
    }

    @Test
    void handlePaymentSettlement_PendingTopUp_ShouldMarkSuccessAndCreditWalletBalance() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-SETTLE-001";
        BigDecimal topUpAmount = new BigDecimal("125000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(topUpOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentSettlement(topUpOrderId);

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("125000.00"), persistedWallet.getBalance());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionType.TOPUP, persistedTopUp.getType());
        assertEquals(TransactionStatus.SUCCESS, persistedTopUp.getStatus());
    }

    @Test
    void handlePaymentSettlement_WhenPaymentAndTopUpShareOrderId_ShouldStillSettlePendingTopUp() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String sharedOrderId = "ORDER-SHARED-001";
        BigDecimal topUpAmount = new BigDecimal("75000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction existingPayment = new Transaction();
        existingPayment.setWalletId(wallet.getWalletId());
        existingPayment.setAmount(new BigDecimal("50000.00"));
        existingPayment.setType(TransactionType.PAYMENT);
        existingPayment.setStatus(TransactionStatus.SUCCESS);
        existingPayment.setDescription(sharedOrderId);
        existingPayment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        existingPayment.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        transactionRepository.save(existingPayment);

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(sharedOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentSettlement(sharedOrderId);

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(topUpAmount, persistedWallet.getBalance());

        Transaction settledTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> sharedOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.SUCCESS, settledTopUp.getStatus());
    }

    @Test
    void handlePaymentFailure_WhenPaymentAndTopUpShareOrderId_ShouldFailPendingTopUpWithoutCreditingBalance() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String sharedOrderId = "ORDER-SHARED-FAIL-001";
        BigDecimal topUpAmount = new BigDecimal("88000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction existingPayment = new Transaction();
        existingPayment.setWalletId(wallet.getWalletId());
        existingPayment.setAmount(new BigDecimal("30000.00"));
        existingPayment.setType(TransactionType.PAYMENT);
        existingPayment.setStatus(TransactionStatus.SUCCESS);
        existingPayment.setDescription(sharedOrderId);
        existingPayment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 8, 0));
        existingPayment.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 8, 0));
        transactionRepository.save(existingPayment);

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(sharedOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentFailure(sharedOrderId);

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        Transaction failedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> sharedOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.FAILED, failedTopUp.getStatus());
    }

    @Test
    void handlePaymentFailure_AfterTopUpAlreadySettled_ShouldBeNoOpAndKeepBalance() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-OUT-OF-ORDER-001";
        BigDecimal topUpAmount = new BigDecimal("99000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(topUpOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentSettlement(topUpOrderId);

        assertDoesNotThrow(() -> walletService.handlePaymentFailure(topUpOrderId));

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(topUpAmount, persistedWallet.getBalance());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.SUCCESS, persistedTopUp.getStatus());
    }

    @Test
    void handlePaymentSettlement_AfterTopUpAlreadyFailed_ShouldBeNoOpAndKeepBalance() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-OUT-OF-ORDER-002";
        BigDecimal topUpAmount = new BigDecimal("45000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(topUpOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 13, 0));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 13, 0));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentFailure(topUpOrderId);

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement(topUpOrderId));

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.FAILED, persistedTopUp.getStatus());
    }

    @Test
    void handlePaymentSettlement_NonTopUpOrderIdWithPendingPaymentAndTopUp_ShouldPrioritizePayment() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String sharedOrderId = "ORDER-NON-TOPUP-001";
        BigDecimal topUpAmount = new BigDecimal("120000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingPayment = new Transaction();
        pendingPayment.setWalletId(wallet.getWalletId());
        pendingPayment.setAmount(new BigDecimal("50000.00"));
        pendingPayment.setType(TransactionType.PAYMENT);
        pendingPayment.setStatus(TransactionStatus.PENDING);
        pendingPayment.setDescription(sharedOrderId);
        pendingPayment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 15, 0));
        pendingPayment.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 15, 0));
        transactionRepository.save(pendingPayment);

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(sharedOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 15, 1));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 15, 1));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentSettlement(sharedOrderId);

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        Transaction persistedPayment = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .filter(transaction -> sharedOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.SUCCESS, persistedPayment.getStatus());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> sharedOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.PENDING, persistedTopUp.getStatus());
    }

    @Test
    void handlePaymentFailure_NonTopUpOrderIdWithPendingPaymentAndTopUp_ShouldPrioritizePayment() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String sharedOrderId = "ORDER-NON-TOPUP-FAIL-001";
        BigDecimal topUpAmount = new BigDecimal("120000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingPayment = new Transaction();
        pendingPayment.setWalletId(wallet.getWalletId());
        pendingPayment.setAmount(new BigDecimal("50000.00"));
        pendingPayment.setType(TransactionType.PAYMENT);
        pendingPayment.setStatus(TransactionStatus.PENDING);
        pendingPayment.setDescription(sharedOrderId);
        pendingPayment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 16, 0));
        pendingPayment.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 16, 0));
        transactionRepository.save(pendingPayment);

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(sharedOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 16, 1));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 16, 1));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentFailure(sharedOrderId);

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        Transaction persistedPayment = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .filter(transaction -> sharedOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.FAILED, persistedPayment.getStatus());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> sharedOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.PENDING, persistedTopUp.getStatus());
    }

    @Test
    void handlePaymentSettlement_TopUpOrderIdWithoutPendingTopUp_ShouldNotFallbackToPayment() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-NO-PENDING-001";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingPayment = new Transaction();
        pendingPayment.setWalletId(wallet.getWalletId());
        pendingPayment.setAmount(new BigDecimal("50000.00"));
        pendingPayment.setType(TransactionType.PAYMENT);
        pendingPayment.setStatus(TransactionStatus.PENDING);
        pendingPayment.setDescription(topUpOrderId);
        pendingPayment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 17, 0));
        pendingPayment.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 17, 0));
        transactionRepository.save(pendingPayment);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> walletService.handlePaymentSettlement(topUpOrderId)
        );
        assertEquals("Pending topup transaction not found for orderId: " + topUpOrderId, exception.getMessage());

        Transaction persistedPayment = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.PENDING, persistedPayment.getStatus());
    }

    @Test
    void handlePaymentFailure_TopUpOrderIdWithoutPendingTopUp_ShouldNotFallbackToPayment() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-NO-PENDING-FAIL-001";

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingPayment = new Transaction();
        pendingPayment.setWalletId(wallet.getWalletId());
        pendingPayment.setAmount(new BigDecimal("50000.00"));
        pendingPayment.setType(TransactionType.PAYMENT);
        pendingPayment.setStatus(TransactionStatus.PENDING);
        pendingPayment.setDescription(topUpOrderId);
        pendingPayment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 18, 0));
        pendingPayment.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 18, 0));
        transactionRepository.save(pendingPayment);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> walletService.handlePaymentFailure(topUpOrderId)
        );
        assertEquals("Pending topup transaction not found for orderId: " + topUpOrderId, exception.getMessage());

        Transaction persistedPayment = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.PENDING, persistedPayment.getStatus());
    }

    @Test
    void handlePaymentSettlement_NonTopUpOrderIdWithOnlyPendingTopUp_ShouldNotSettleTopUp() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String nonTopUpOrderId = "ORDER-NON-TOPUP-ONLY-TOPUP-001";
        BigDecimal topUpAmount = new BigDecimal("67000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(nonTopUpOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 19, 0));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 19, 0));
        transactionRepository.save(pendingTopUp);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> walletService.handlePaymentSettlement(nonTopUpOrderId)
        );
        assertEquals("Pending payment transaction not found for orderId: " + nonTopUpOrderId, exception.getMessage());

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> nonTopUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.PENDING, persistedTopUp.getStatus());
    }

    @Test
    void handlePaymentFailure_TopUpOrderIdWithPendingPaymentAndTopUp_ShouldPrioritizeTopUp() {
        UUID userId = UUID.randomUUID();
        WalletResponse walletResponse = walletService.createWallet(userId);
        UUID walletId = walletResponse.getWalletId();
        String topUpOrderId = "TOPUP-FAIL-PRIORITY-001";
        BigDecimal topUpAmount = new BigDecimal("77000.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        Transaction pendingPayment = new Transaction();
        pendingPayment.setWalletId(wallet.getWalletId());
        pendingPayment.setAmount(new BigDecimal("50000.00"));
        pendingPayment.setType(TransactionType.PAYMENT);
        pendingPayment.setStatus(TransactionStatus.PENDING);
        pendingPayment.setDescription(topUpOrderId);
        pendingPayment.setCreatedAt(LocalDateTime.of(2026, 5, 1, 20, 0));
        pendingPayment.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 20, 0));
        transactionRepository.save(pendingPayment);

        Transaction pendingTopUp = new Transaction();
        pendingTopUp.setWalletId(wallet.getWalletId());
        pendingTopUp.setAmount(topUpAmount);
        pendingTopUp.setType(TransactionType.TOPUP);
        pendingTopUp.setStatus(TransactionStatus.PENDING);
        pendingTopUp.setDescription(topUpOrderId);
        pendingTopUp.setCreatedAt(LocalDateTime.of(2026, 5, 1, 20, 1));
        pendingTopUp.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 20, 1));
        transactionRepository.save(pendingTopUp);

        walletService.handlePaymentFailure(topUpOrderId);

        Wallet persistedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), persistedWallet.getBalance());

        Transaction persistedTopUp = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.TOPUP)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.FAILED, persistedTopUp.getStatus());

        Transaction persistedPayment = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getType() == TransactionType.PAYMENT)
                .filter(transaction -> topUpOrderId.equals(transaction.getDescription()))
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionStatus.PENDING, persistedPayment.getStatus());
    }

    private void pauseForDistinctPersistTimestamp() {
        LockSupport.parkNanos(5_000_000L);
    }
}
