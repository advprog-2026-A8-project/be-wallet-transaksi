package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletRequestAccessPolicyTest {

    private static final String JWT_SECRET =
            "DefaultSecretKeyUntukDevelopmentLokalYangSangatPanjangSekali123!@#";

    @Test
    void isValidReadJwt_ShouldRejectLegacySentinelToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertFalse(policy.isValidReadJwt("Bearer valid-read-jwt"));
    }

    @Test
    void isValidJastiperJwt_ShouldRejectLegacySentinelToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertFalse(policy.isValidJastiperJwt("Bearer valid-jastiper-jwt"));
    }

    @Test
    void isOwnerMismatchToken_ShouldRejectLegacySentinelToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertFalse(policy.isOwnerMismatchToken("Bearer valid-non-admin-other-user"));
    }

    @Test
    void isInvalidJwtToken_ShouldBeTrueForInvalidJwtStructure() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertTrue(policy.isInvalidJwtToken("Bearer invalid.jwt.token"));
    }

    @Test
    void isInvalidJwtToken_ShouldReturnTrueForMalformedJwtStructure() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertTrue(policy.isInvalidJwtToken("Bearer aaa.bbb.ccc"));
    }

    @Test
    void isInvalidJwtToken_ShouldReturnFalseForNonJwtBearerToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertFalse(policy.isInvalidJwtToken("Bearer invalid-jwt-token"));
    }

    @Test
    void isForbiddenTopUpRole_ShouldRejectLegacyJastiperSentinel() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);

        assertFalse(policy.isForbiddenTopUpRole("Bearer valid-jastiper", "JASTIPER"));
    }
}
