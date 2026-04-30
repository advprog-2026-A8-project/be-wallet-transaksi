package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MidtransCallbackSignatureVerifier {

    private final String midtransServerKey;

    public MidtransCallbackSignatureVerifier(@Value("${midtrans.server-key:}") String midtransServerKey) {
        this.midtransServerKey = midtransServerKey == null ? "" : midtransServerKey;
    }

    public boolean isValid(PaymentCallbackRequest payload, String signatureKey) {
        if (!isVerifiableInput(payload, signatureKey)) {
            return false;
        }
        try {
            String expectedSignature = buildExpectedSignature(payload);
            return expectedSignature.equals(normalizeSignature(signatureKey));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isVerifiableInput(PaymentCallbackRequest payload, String signatureKey) {
        return payload != null && signatureKey != null && !signatureKey.isBlank();
    }

    private String normalizeSignature(String signature) {
        return signature.trim().toLowerCase(Locale.ROOT);
    }

    private String buildExpectedSignature(PaymentCallbackRequest payload) {
        String orderId = requiredValue(payload.getOrderId(), "order_id");
        String statusCode = requiredValue(payload.getStatusCode(), "status_code");
        String grossAmount = requiredValue(payload.getGrossAmount(), "gross_amount");
        String rawSignature = orderId + statusCode + grossAmount + midtransServerKey;
        return sha512Hex(rawSignature);
    }

    private String requiredValue(String value, String key) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required callback field: " + key);
        }
        String text = value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Missing required callback field: " + key);
        }
        return text;
    }

    private String sha512Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-512 algorithm is unavailable", ex);
        }
    }
}
