package id.ac.ui.cs.advprog.bewallettransaksi.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    void testWalletGettersAndSetters() {
        Wallet wallet = new Wallet();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal balance = BigDecimal.valueOf(500.50);

        wallet.setWalletId(walletId);
        wallet.setUserId(userId);
        wallet.setBalance(balance);

        assertEquals(walletId, wallet.getWalletId());
        assertEquals(userId, wallet.getUserId());
        assertEquals(balance, wallet.getBalance());
    }

    @Test
    void testWalletDefaultBalance() {
        Wallet wallet = new Wallet();
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
    }

    @Test
    void testWalletNullValues() {
        Wallet wallet = new Wallet();
        assertNull(wallet.getWalletId());
        assertNull(wallet.getUserId());
    }
}
