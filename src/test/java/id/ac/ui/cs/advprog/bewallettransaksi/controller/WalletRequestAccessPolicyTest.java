package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class WalletRequestAccessPolicyTest {

    private static final String JWT_SECRET =
            "DefaultSecretKeyUntukDevelopmentLokalYangSangatPanjangSekali123!@#";

    @Test
    void isValidReadJwt_ShouldRejectLegacySentinelToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());

        assertFalse(policy.isValidReadJwt("Bearer valid-read-jwt"));
    }

    @Test
    void isValidJastiperJwt_ShouldRejectLegacySentinelToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());

        assertFalse(policy.isValidJastiperJwt("Bearer valid-jastiper-jwt"));
    }

    @Test
    void isOwnerMismatchToken_ShouldRejectLegacySentinelToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());

        assertFalse(policy.isOwnerMismatchToken());
    }

    @Test
    void isInvalidJwtToken_ShouldBeTrueForInvalidJwtStructure() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());

        assertTrue(policy.isInvalidJwtToken("Bearer invalid.jwt.token"));
    }

    @Test
    void isInvalidJwtToken_ShouldReturnTrueForMalformedJwtStructure() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());

        assertTrue(policy.isInvalidJwtToken("Bearer aaa.bbb.ccc"));
    }

    @Test
    void isInvalidJwtToken_ShouldReturnFalseForNonJwtBearerToken() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());

        assertFalse(policy.isInvalidJwtToken("Bearer invalid-jwt-token"));
    }

    @Test
    void isForbiddenTopUpRole_ShouldRejectLegacyJastiperSentinel() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());

        assertFalse(policy.isForbiddenTopUpRole("Bearer valid-jastiper"));
    }

    @Test
    void isOwnerMismatchJwt_ShouldReturnTrueForNonAdminWithInvalidUuidSubject() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());
        String jwt = generateJwtToken("not-a-uuid", "TITIPER");

        assertTrue(policy.isOwnerMismatchJwt("Bearer " + jwt, UUID.randomUUID()));
    }

    @Test
    void isOwnerMismatchJwt_ShouldReturnFalseForAdminWithInvalidUuidSubject() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> java.util.Optional.empty());
        String jwt = generateJwtToken("not-a-uuid", "ADMIN");

        assertFalse(policy.isOwnerMismatchJwt("Bearer " + jwt, UUID.randomUUID()));
    }

    private String generateJwtToken(String subject, String role) {
        return TestJwtTokenFactory.generateHmac256Token(JWT_SECRET, subject, role);
    }
}

