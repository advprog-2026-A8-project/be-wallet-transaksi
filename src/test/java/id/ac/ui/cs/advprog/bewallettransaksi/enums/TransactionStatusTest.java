package id.ac.ui.cs.advprog.bewallettransaksi.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStatusTest {

    @Test
    void testEnumValues() {
        TransactionStatus[] values = TransactionStatus.values();
        assertEquals(3, values.length);
        assertEquals(TransactionStatus.PENDING, values[0]);
        assertEquals(TransactionStatus.SUCCESS, values[1]);
        assertEquals(TransactionStatus.FAILED, values[2]);
    }

    @Test
    void testGetValue() {
        assertEquals("PENDING", TransactionStatus.PENDING.getValue());
        assertEquals("SUCCESS", TransactionStatus.SUCCESS.getValue());
        assertEquals("FAILED", TransactionStatus.FAILED.getValue());
    }

    @Test
    void testContains_ExistingValues() {
        assertTrue(TransactionStatus.contains("PENDING"));
        assertTrue(TransactionStatus.contains("SUCCESS"));
        assertTrue(TransactionStatus.contains("FAILED"));
    }

    @Test
    void testContains_NonExistingValues() {
        assertFalse(TransactionStatus.contains("INVALID"));
        assertFalse(TransactionStatus.contains("pending"));
        assertFalse(TransactionStatus.contains(""));
        assertFalse(TransactionStatus.contains("COMPLETED"));
    }

    @Test
    void testValueOf() {
        assertEquals(TransactionStatus.PENDING, TransactionStatus.valueOf("PENDING"));
        assertEquals(TransactionStatus.SUCCESS, TransactionStatus.valueOf("SUCCESS"));
        assertEquals(TransactionStatus.FAILED, TransactionStatus.valueOf("FAILED"));
    }

    @Test
    void testValueOf_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> TransactionStatus.valueOf("INVALID"));
    }
}
