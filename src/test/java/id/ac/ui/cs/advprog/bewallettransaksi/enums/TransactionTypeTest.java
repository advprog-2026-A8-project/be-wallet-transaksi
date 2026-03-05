package id.ac.ui.cs.advprog.bewallettransaksi.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TransactionTypeTest {

    @Test
    void testEnumValues() {
        TransactionType[] values = TransactionType.values();
        assertEquals(4, values.length);
        assertEquals(TransactionType.TOPUP, values[0]);
        assertEquals(TransactionType.PAYMENT, values[1]);
        assertEquals(TransactionType.REFUND, values[2]);
        assertEquals(TransactionType.WITHDRAW, values[3]);
    }

    @Test
    void testGetValue() {
        assertEquals("TOPUP", TransactionType.TOPUP.getValue());
        assertEquals("PAYMENT", TransactionType.PAYMENT.getValue());
        assertEquals("REFUND", TransactionType.REFUND.getValue());
        assertEquals("WITHDRAW", TransactionType.WITHDRAW.getValue());
    }

    @Test
    void testContains_ExistingValues() {
        assertTrue(TransactionType.contains("TOPUP"));
        assertTrue(TransactionType.contains("PAYMENT"));
        assertTrue(TransactionType.contains("REFUND"));
        assertTrue(TransactionType.contains("WITHDRAW"));
    }

    @Test
    void testContains_NonExistingValues() {
        assertFalse(TransactionType.contains("INVALID"));
        assertFalse(TransactionType.contains("topup"));
        assertFalse(TransactionType.contains(""));
        assertFalse(TransactionType.contains("TRANSFER"));
    }

    @Test
    void testValueOf() {
        assertEquals(TransactionType.TOPUP, TransactionType.valueOf("TOPUP"));
        assertEquals(TransactionType.PAYMENT, TransactionType.valueOf("PAYMENT"));
        assertEquals(TransactionType.REFUND, TransactionType.valueOf("REFUND"));
        assertEquals(TransactionType.WITHDRAW, TransactionType.valueOf("WITHDRAW"));
    }

    @Test
    void testValueOf_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> TransactionType.valueOf("INVALID"));
    }
}
