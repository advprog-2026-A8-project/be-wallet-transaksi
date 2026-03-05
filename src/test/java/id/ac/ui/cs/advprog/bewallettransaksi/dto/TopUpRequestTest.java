package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TopUpRequestTest {

    @Test
    void testGettersAndSetters() {
        TopUpRequest request = new TopUpRequest();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100.00);

        request.setUserId(userId);
        request.setAmount(amount);

        assertEquals(userId, request.getUserId());
        assertEquals(amount, request.getAmount());
    }

    @Test
    void testNullValues() {
        TopUpRequest request = new TopUpRequest();
        assertNull(request.getUserId());
        assertNull(request.getAmount());
    }

    @Test
    void testUpdateValues() {
        TopUpRequest request = new TopUpRequest();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        request.setUserId(userId1);
        request.setAmount(BigDecimal.valueOf(50.00));

        request.setUserId(userId2);
        request.setAmount(BigDecimal.valueOf(100.00));

        assertEquals(userId2, request.getUserId());
        assertEquals(BigDecimal.valueOf(100.00), request.getAmount());
    }
}
