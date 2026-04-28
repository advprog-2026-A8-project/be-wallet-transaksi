package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WalletRequestAccessPolicyTest {

    private static final String JWT_SECRET =
            "DefaultSecretKeyUntukDevelopmentLokalYangSangatPanjangSekali123!@#";

    @Test
    void isValidReadJwt_ShouldRejectLegacySentinelToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertFalse(policy.isValidReadJwt("Bearer valid-read-jwt"));
    }
}
