package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MidtransCallbackSignatureVerifierTest {

    @Test
    void isValid_NullPayload_ShouldReturnFalseWithoutThrowing() {
        MidtransCallbackSignatureVerifier verifier = new MidtransCallbackSignatureVerifier("server-key");

        assertDoesNotThrow(() -> assertFalse(verifier.isValid(null, "any-signature")));
    }
}
