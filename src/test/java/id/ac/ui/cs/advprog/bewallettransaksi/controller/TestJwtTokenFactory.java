package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class TestJwtTokenFactory {

    private TestJwtTokenFactory() {
    }

    static String generateHmac256Token(String secret, String subject, String role) {
        long issuedAtSeconds = System.currentTimeMillis() / 1000L;
        long expirationSeconds = issuedAtSeconds + 86_400L;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"" + escapeJson(subject) + "\",\"role\":\"" + escapeJson(role)
                + "\",\"iat\":" + issuedAtSeconds + ",\"exp\":" + expirationSeconds + "}";

        String encodedHeader = base64Url(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;

        return signingInput + "." + base64Url(hmacSha256(secret.getBytes(StandardCharsets.UTF_8), signingInput));
    }

    private static byte[] hmacSha256(byte[] secret, String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate test JWT", exception);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}