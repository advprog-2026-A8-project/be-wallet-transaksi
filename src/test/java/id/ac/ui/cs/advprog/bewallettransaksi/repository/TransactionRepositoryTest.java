package id.ac.ui.cs.advprog.bewallettransaksi.repository;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findByWalletIdOrderByCreatedAtDesc_ReturnsLatestFirst() {
        UUID walletId = UUID.randomUUID();
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0, 0);

        Transaction oldest = createTransaction(walletId, BigDecimal.valueOf(10), TransactionType.TOPUP,
                TransactionStatus.SUCCESS, "oldest", baseTime);
        transactionRepository.save(oldest);

        Transaction middle = createTransaction(walletId, BigDecimal.valueOf(20), TransactionType.PAYMENT,
                TransactionStatus.SUCCESS, "middle", baseTime.plusMinutes(1));
        transactionRepository.save(middle);

        Transaction latest = createTransaction(walletId, BigDecimal.valueOf(30), TransactionType.REFUND,
                TransactionStatus.SUCCESS, "latest", baseTime.plusMinutes(2));
        transactionRepository.save(latest);

        List<Transaction> histories = transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId);

        assertEquals(3, histories.size());
        assertEquals("latest", histories.get(0).getDescription());
        assertEquals("middle", histories.get(1).getDescription());
        assertEquals("oldest", histories.get(2).getDescription());
    }

    @Test
    void findByWalletIdAndStatusOrderByCreatedAtDesc_ReturnsFilteredStatus() {
        UUID walletId = UUID.randomUUID();

        transactionRepository.save(createTransaction(walletId, BigDecimal.valueOf(40), TransactionType.PAYMENT,
                TransactionStatus.SUCCESS, "success-payment", LocalDateTime.of(2026, 1, 1, 10, 0, 0)));
        transactionRepository.save(createTransaction(walletId, BigDecimal.valueOf(50), TransactionType.WITHDRAW,
                TransactionStatus.FAILED, "failed-withdraw", LocalDateTime.of(2026, 1, 1, 10, 1, 0)));
        transactionRepository.save(createTransaction(walletId, BigDecimal.valueOf(60), TransactionType.REFUND,
                TransactionStatus.SUCCESS, "success-refund", LocalDateTime.of(2026, 1, 1, 10, 2, 0)));

        List<Transaction> successHistories = transactionRepository
                .findByWalletIdAndStatusOrderByCreatedAtDesc(walletId, TransactionStatus.SUCCESS);

        assertEquals(2, successHistories.size());
        assertEquals(TransactionStatus.SUCCESS, successHistories.get(0).getStatus());
        assertEquals(TransactionStatus.SUCCESS, successHistories.get(1).getStatus());
    }

    private Transaction createTransaction(UUID walletId, BigDecimal amount, TransactionType type,
                                          TransactionStatus status, String description, LocalDateTime createdAt) {
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setDescription(description);
        transaction.setCreatedAt(createdAt);
        transaction.setUpdatedAt(createdAt);
        return transaction;
    }
}
