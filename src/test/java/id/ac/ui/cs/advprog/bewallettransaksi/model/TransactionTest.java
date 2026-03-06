package id.ac.ui.cs.advprog.bewallettransaksi.model;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testTransactionGettersAndSetters() {
        Transaction transaction = new Transaction();
        UUID transactionId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100.00);
        TransactionType type = TransactionType.TOPUP;
        TransactionStatus status = TransactionStatus.SUCCESS;
        String description = "Test transaction";
        LocalDateTime now = LocalDateTime.now();

        transaction.setTransactionId(transactionId);
        transaction.setWalletId(walletId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setDescription(description);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        assertEquals(transactionId, transaction.getTransactionId());
        assertEquals(walletId, transaction.getWalletId());
        assertEquals(amount, transaction.getAmount());
        assertEquals(type, transaction.getType());
        assertEquals(status, transaction.getStatus());
        assertEquals(description, transaction.getDescription());
        assertEquals(now, transaction.getCreatedAt());
        assertEquals(now, transaction.getUpdatedAt());
    }

    @Test
    void testOnCreate() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getCreatedAt());
        assertNull(transaction.getUpdatedAt());

        transaction.onCreate();

        assertNotNull(transaction.getCreatedAt());
        assertNotNull(transaction.getUpdatedAt());
        assertEquals(transaction.getCreatedAt(), transaction.getUpdatedAt());
    }

    @Test
    void testOnUpdate() throws InterruptedException {
        Transaction transaction = new Transaction();
        transaction.onCreate();
        LocalDateTime originalCreatedAt = transaction.getCreatedAt();
        LocalDateTime originalUpdatedAt = transaction.getUpdatedAt();

        // Small delay to ensure different timestamp
        Thread.sleep(10);

        transaction.onUpdate();

        assertEquals(originalCreatedAt, transaction.getCreatedAt());
        assertTrue(transaction.getUpdatedAt().isAfter(originalUpdatedAt) 
                || transaction.getUpdatedAt().equals(originalUpdatedAt));
    }

    @Test
    void testTransactionNullValues() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getTransactionId());
        assertNull(transaction.getWalletId());
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getStatus());
        assertNull(transaction.getDescription());
        assertNull(transaction.getCreatedAt());
        assertNull(transaction.getUpdatedAt());
    }

    @Test
    void testTransactionWithAllTypes() {
        Transaction transaction = new Transaction();
        
        transaction.setType(TransactionType.TOPUP);
        assertEquals(TransactionType.TOPUP, transaction.getType());
        
        transaction.setType(TransactionType.PAYMENT);
        assertEquals(TransactionType.PAYMENT, transaction.getType());
        
        transaction.setType(TransactionType.REFUND);
        assertEquals(TransactionType.REFUND, transaction.getType());
        
        transaction.setType(TransactionType.WITHDRAW);
        assertEquals(TransactionType.WITHDRAW, transaction.getType());
    }

    @Test
    void testTransactionWithAllStatuses() {
        Transaction transaction = new Transaction();
        
        transaction.setStatus(TransactionStatus.PENDING);
        assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        
        transaction.setStatus(TransactionStatus.SUCCESS);
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        
        transaction.setStatus(TransactionStatus.FAILED);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
    }
}
