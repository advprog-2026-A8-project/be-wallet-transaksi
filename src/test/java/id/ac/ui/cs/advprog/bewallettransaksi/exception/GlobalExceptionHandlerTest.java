package id.ac.ui.cs.advprog.bewallettransaksi.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleWalletNotFound() {
        UUID userId = UUID.randomUUID();
        WalletNotFoundException exception = new WalletNotFoundException(userId);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleWalletNotFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals("Wallet not found for user: " + userId, response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void testHandleInvalidAmount() {
        String message = "Amount must be greater than zero";
        InvalidAmountException exception = new InvalidAmountException(message);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidAmount(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals(message, response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void testResponseContainsAllRequiredFields() {
        InvalidAmountException exception = new InvalidAmountException("Test");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleInvalidAmount(exception);
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertEquals(4, body.size());
    }
}
