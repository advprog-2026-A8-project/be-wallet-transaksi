package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletResponseTest {

    @Test
    void testBuilder() {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal balance = BigDecimal.valueOf(250.75);

        WalletResponse response = WalletResponse.builder()
                .walletId(walletId)
                .userId(userId)
                .balance(balance)
                .build();

        assertEquals(walletId, response.getWalletId());
        assertEquals(userId, response.getUserId());
        assertEquals(balance, response.getBalance());
    }

    @Test
    void testBuilderWithNullValues() {
        WalletResponse response = WalletResponse.builder()
                .walletId(null)
                .userId(null)
                .balance(null)
                .build();

        assertNull(response.getWalletId());
        assertNull(response.getUserId());
        assertNull(response.getBalance());
    }

    @Test
    void testBuilderWithZeroBalance() {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        WalletResponse response = WalletResponse.builder()
                .walletId(walletId)
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();

        assertEquals(BigDecimal.ZERO, response.getBalance());
    }
}
