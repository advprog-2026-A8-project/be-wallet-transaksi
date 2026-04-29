package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MidtransCallbackSignatureVerifier {

    private final String midtransServerKey;

    public MidtransCallbackSignatureVerifier(@Value("${midtrans.server-key:}") String midtransServerKey) {
        this.midtransServerKey = midtransServerKey == null ? "" : midtransServerKey;
    }

    public boolean isValid(Map<String, Object> payload, String signatureKey) {
        String expectedSignature = buildExpectedSignature(payload);
        return expectedSignature.equals(signatureKey);
    }

    private String buildExpectedSignature(Map<String, Object> payload) {
        String orderId = extractRequiredPayloadValue(payload, "order_id");
        String statusCode = extractRequiredPayloadValue(payload, "status_code");
        String grossAmount = extractRequiredPayloadValue(payload, "gross_amount");
        String rawSignature = orderId + statusCode + grossAmount + midtransServerKey;
        return sha512Hex(rawSignature);
    }

    private String extractRequiredPayloadValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required callback field: " + key);
        }
        String text = value.toString().trim();
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
