package id.ac.ui.cs.advprog.bewallettransaksi.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidAmountExceptionTest {

    @Test
    void testExceptionMessage() {
        String message = "Amount must be greater than zero";
        InvalidAmountException exception = new InvalidAmountException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testExceptionIsRuntimeException() {
        InvalidAmountException exception = new InvalidAmountException("Test message");

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testExceptionWithDifferentMessages() {
        String message1 = "Amount cannot be null";
        String message2 = "Amount must be positive";

        InvalidAmountException exception1 = new InvalidAmountException(message1);
        InvalidAmountException exception2 = new InvalidAmountException(message2);

        assertEquals(message1, exception1.getMessage());
        assertEquals(message2, exception2.getMessage());
        assertNotEquals(exception1.getMessage(), exception2.getMessage());
    }

    @Test
    void testExceptionWithNullMessage() {
        InvalidAmountException exception = new InvalidAmountException(null);
        assertNull(exception.getMessage());
    }
}
