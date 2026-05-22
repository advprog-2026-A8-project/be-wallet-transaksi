package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.UUID;
import java.util.Optional;

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

    @Test
    void isOwnerMismatchJwt_ShouldResolveUsernameSubjectViaResolver() {
        UUID ownerId = UUID.randomUUID();
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(
                JWT_SECRET,
                username -> "owner@example.com".equals(username) ? Optional.of(ownerId) : Optional.empty()
        );
        String jwt = generateJwtToken("owner@example.com", "TITIPER");

        assertFalse(policy.isOwnerMismatchJwt("Bearer " + jwt, ownerId));
    }

    @Test
    void isAllowedPayRole_ShouldReturnTrueForTitiperAndAdmin() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> Optional.empty());
        String titiperJwt = "Bearer " + generateJwtToken("user-1", "TITIPER");
        String adminJwt = "Bearer " + generateJwtToken("user-2", "ADMIN");

        assertTrue(policy.isAllowedPayRole(titiperJwt));
        assertTrue(policy.isAllowedPayRole(adminJwt));
    }

    @Test
    void isAllowedWalletMutationRole_ShouldReturnFalseForUnsupportedRole() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> Optional.empty());
        String jwt = "Bearer " + generateJwtToken("user-3", "VIEWER");

        assertFalse(policy.isAllowedWalletMutationRole(jwt));
    }

    @Test
    void isJwtBearerToken_ShouldValidateJwtShapeOnly() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET, username -> Optional.empty());
        String validShape = "Bearer aaa.bbb.ccc";
        String invalidShape = "Bearer no-dot-token";

        assertTrue(policy.isJwtBearerToken(validShape));
        assertFalse(policy.isJwtBearerToken(invalidShape));
    }

    private String generateJwtToken(String subject, String role) {
        return TestJwtTokenFactory.generateHmac256Token(JWT_SECRET, subject, role);
    }
}

