package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.InvalidAmountException;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
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
        assertEquals(TransactionType.WITHDRAW, history.get(0).getType());
        assertEquals(TransactionType.REFUND, history.get(1).getType());
        assertEquals(TransactionType.PAYMENT, history.get(2).getType());
        assertEquals(TransactionType.TOPUP, history.get(3).getType());
        assertEquals(TransactionStatus.SUCCESS, history.get(0).getStatus());
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
}

