package id.ac.ui.cs.advprog.bewallettransaksi.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletNotFoundExceptionTest {

    @Test
    void testExceptionMessage() {
        UUID userId = UUID.randomUUID();
        WalletNotFoundException exception = new WalletNotFoundException(userId);

        assertEquals("Wallet not found for user: " + userId, exception.getMessage());
    }

    @Test
    void testExceptionIsRuntimeException() {
        UUID userId = UUID.randomUUID();
        WalletNotFoundException exception = new WalletNotFoundException(userId);

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testExceptionWithDifferentUserIds() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        WalletNotFoundException exception1 = new WalletNotFoundException(userId1);
        WalletNotFoundException exception2 = new WalletNotFoundException(userId2);

        assertNotEquals(exception1.getMessage(), exception2.getMessage());
        assertTrue(exception1.getMessage().contains(userId1.toString()));
        assertTrue(exception2.getMessage().contains(userId2.toString()));
    }
}
