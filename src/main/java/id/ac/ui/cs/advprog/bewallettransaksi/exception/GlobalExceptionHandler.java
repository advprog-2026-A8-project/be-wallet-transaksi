package id.ac.ui.cs.advprog.bewallettransaksi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DATA_INTEGRITY_VIOLATION_MESSAGE = "Data integrity violation";
    private static final String TYPE_MISMATCH_MESSAGE_TEMPLATE = "Invalid value '%s' for parameter '%s'";
    private static final String MISSING_PARAMETER_MESSAGE_TEMPLATE = "Missing required request parameter: %s";

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAmount(InvalidAmountException ex) {
        return handleBadRequest(ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return handleBadRequest(ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return handleBadRequest(ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = extractFirstValidationMessage(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = buildTypeMismatchMessage(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, DATA_INTEGRITY_VIOLATION_MESSAGE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex
    ) {
        return handleBadRequest(buildMissingParameterMessage(ex));
    }

    private ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return handleBadRequest(ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> handleBadRequest(String message) {
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    private String extractFirstValidationMessage(MethodArgumentNotValidException ex) {
        return ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation failed")
                .orElse("Validation failed");
    }

    private String buildTypeMismatchMessage(MethodArgumentTypeMismatchException ex) {
        return String.format(TYPE_MISMATCH_MESSAGE_TEMPLATE, ex.getValue(), ex.getName());
    }

    private String buildMissingParameterMessage(MissingServletRequestParameterException ex) {
        return String.format(MISSING_PARAMETER_MESSAGE_TEMPLATE, ex.getParameterName());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
