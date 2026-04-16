package id.ac.ui.cs.advprog.bewallettransaksi.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @Test
    void testHandleIllegalArgument() {
        IllegalArgumentException exception = new IllegalArgumentException("Bad input");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad input", response.getBody().get("message"));
    }

    @Test
    void testHandleDataIntegrityViolation() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDataIntegrityViolation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Data integrity violation", response.getBody().get("message"));
    }

    @Test
    void testHandleTypeMismatch() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "INVALID_STATUS",
                String.class,
                "status",
                null,
                new IllegalArgumentException("bad enum")
        );

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleTypeMismatch(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid value 'INVALID_STATUS' for parameter 'status'", response.getBody().get("message"));
    }

    @Test
    void testHandleValidationException_WhenDefaultMessageNull_ShouldReturnFallbackMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request",
                "amount",
                "invalid",
                false,
                null,
                null,
                null
        ));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                Mockito.mock(MethodParameter.class),
                bindingResult
        );

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation failed", response.getBody().get("message"));
    }
}
