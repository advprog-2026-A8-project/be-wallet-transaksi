package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MidtransCallbackSignatureVerifierTest {

    @Test
    void isValid_NullPayload_ShouldReturnFalseWithoutThrowing() {
        MidtransCallbackSignatureVerifier verifier = new MidtransCallbackSignatureVerifier("server-key");

        assertDoesNotThrow(() -> assertFalse(verifier.isValid(null, "any-signature")));
    }

    @Test
    void isValid_UppercaseHexSignature_ShouldStillBeValid() {
        String serverKey = "server-key";
        MidtransCallbackSignatureVerifier verifier = new MidtransCallbackSignatureVerifier(serverKey);
        PaymentCallbackRequest payload = new PaymentCallbackRequest();
        payload.setOrderId("ORDER-HEX-1");
        payload.setStatusCode("200");
        payload.setGrossAmount("10000.00");
        String rawSignature = payload.getOrderId() + payload.getStatusCode() + payload.getGrossAmount() + serverKey;
        String uppercaseSignature = sha512Hex(rawSignature).toUpperCase();

        assertTrue(verifier.isValid(payload, uppercaseSignature));
    }

    @Test
    void isValid_WhitespaceOnlySignature_ShouldReturnFalse() {
        MidtransCallbackSignatureVerifier verifier = new MidtransCallbackSignatureVerifier("server-key");
        PaymentCallbackRequest payload = new PaymentCallbackRequest();
        payload.setOrderId("ORDER-HEX-2");
        payload.setStatusCode("200");
        payload.setGrossAmount("10000.00");

        assertFalse(verifier.isValid(payload, "   "));
    }

    @Test
    void isValid_BlankServerKey_ShouldReturnFalse() {
        String blankServerKey = "   ";
        MidtransCallbackSignatureVerifier verifier = new MidtransCallbackSignatureVerifier(blankServerKey);
        PaymentCallbackRequest payload = new PaymentCallbackRequest();
        payload.setOrderId("ORDER-NO-KEY-1");
        payload.setStatusCode("200");
        payload.setGrossAmount("10000.00");
        String forgedSignature = sha512Hex(
                payload.getOrderId() + payload.getStatusCode() + payload.getGrossAmount() + blankServerKey
        );

        assertFalse(verifier.isValid(payload, forgedSignature));
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
