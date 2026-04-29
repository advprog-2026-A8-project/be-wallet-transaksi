package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class WalletServiceConcurrencyTest {

    private static final int WORKER_COUNT = 20;
    private static final int OPERATIONS_PER_WORKER = 10;
    private static final BigDecimal TOP_UP_AMOUNT = BigDecimal.TEN;
    private static final BigDecimal WITHDRAW_AMOUNT = new BigDecimal("80.00");

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
    void concurrentTopUp_ShouldAccumulateAllChanges() throws Exception {
        UUID userId = UUID.randomUUID();
        walletService.createWallet(userId);

        int topUpOperations = WORKER_COUNT * OPERATIONS_PER_WORKER;
        BigDecimal expectedBalance = TOP_UP_AMOUNT.multiply(BigDecimal.valueOf(topUpOperations));

        ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(WORKER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < WORKER_COUNT; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                for (int j = 0; j < OPERATIONS_PER_WORKER; j++) {
                    walletService.topUp(buildTopUpRequest(userId));
                }
                return null;
            }));
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(0, expectedBalance.compareTo(wallet.getBalance()));
    }

    @Test
    void concurrentWithdraw_ShouldPreventNegativeBalanceAndPersistFailure() throws Exception {
        UUID userId = UUID.randomUUID();
        walletService.createWallet(userId);

        TopUpRequest topUpRequest = new TopUpRequest();
        topUpRequest.setUserId(userId);
        topUpRequest.setAmount(new BigDecimal("100.00"));
        walletService.topUp(topUpRequest);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<?> first = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            walletService.withdraw(userId, WITHDRAW_AMOUNT, "BCA-111");
            return null;
        });

        Future<?> second = executor.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            walletService.withdraw(userId, WITHDRAW_AMOUNT, "BCA-222");
            return null;
        });

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();

        int failedCount = 0;
        try {
            first.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            failedCount++;
        }

        try {
            second.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            failedCount++;
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, failedCount);

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertEquals(0, new BigDecimal("20.00").compareTo(wallet.getBalance()));

        List<Transaction> history = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId());
        long successWithdraw = history.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAW && t.getStatus() == TransactionStatus.SUCCESS)
                .count();
        long failedWithdraw = history.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAW && t.getStatus() == TransactionStatus.FAILED)
                .count();

        assertEquals(1, successWithdraw);
        assertEquals(1, failedWithdraw);
    }

    private TopUpRequest buildTopUpRequest(UUID userId) {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(TOP_UP_AMOUNT);
        return request;
    }
}
