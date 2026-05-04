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

    private void pauseForDistinctPersistTimestamp() {
        LockSupport.parkNanos(5_000_000L);
    }
}
