package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionResponseTest {

    @Test
    void testBuilder() {
        UUID transactionId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100.00);
        TransactionType type = TransactionType.TOPUP;
        TransactionStatus status = TransactionStatus.SUCCESS;
        String description = "Test topup";
        LocalDateTime createdAt = LocalDateTime.now();

        TransactionResponse response = TransactionResponse.builder()
                .transactionId(transactionId)
                .walletId(walletId)
                .amount(amount)
                .type(type)
                .status(status)
                .description(description)
                .createdAt(createdAt)
                .build();

        assertEquals(transactionId, response.getTransactionId());
        assertEquals(walletId, response.getWalletId());
        assertEquals(amount, response.getAmount());
        assertEquals(type, response.getType());
        assertEquals(status, response.getStatus());
        assertEquals(description, response.getDescription());
        assertEquals(createdAt, response.getCreatedAt());
    }

    @Test
    void testBuilderWithNullValues() {
        TransactionResponse response = TransactionResponse.builder()
                .transactionId(null)
                .walletId(null)
                .amount(null)
                .type(null)
                .status(null)
                .description(null)
                .createdAt(null)
                .build();

        assertNull(response.getTransactionId());
        assertNull(response.getWalletId());
        assertNull(response.getAmount());
        assertNull(response.getType());
        assertNull(response.getStatus());
        assertNull(response.getDescription());
        assertNull(response.getCreatedAt());
    }

    @Test
    void testBuilderWithDifferentTypesAndStatuses() {
        UUID transactionId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        TransactionResponse paymentResponse = TransactionResponse.builder()
                .transactionId(transactionId)
                .walletId(walletId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PENDING)
                .build();

        assertEquals(TransactionType.PAYMENT, paymentResponse.getType());
        assertEquals(TransactionStatus.PENDING, paymentResponse.getStatus());

        TransactionResponse refundResponse = TransactionResponse.builder()
                .transactionId(transactionId)
                .walletId(walletId)
                .amount(BigDecimal.valueOf(25.00))
                .type(TransactionType.REFUND)
                .status(TransactionStatus.FAILED)
                .build();

        assertEquals(TransactionType.REFUND, refundResponse.getType());
        assertEquals(TransactionStatus.FAILED, refundResponse.getStatus());
    }
}
