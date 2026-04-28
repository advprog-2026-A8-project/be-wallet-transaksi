package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

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

        assertFalse(policy.isForbiddenTopUpRole("Bearer valid-jastiper"));
    }

    @Test
    void isOwnerMismatchJwt_ShouldReturnTrueForNonAdminWithInvalidUuidSubject() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);
        String jwt = generateJwtToken("not-a-uuid", "TITIPER");

        assertTrue(policy.isOwnerMismatchJwt("Bearer " + jwt, UUID.randomUUID()));
    }

    @Test
    void isOwnerMismatchJwt_ShouldReturnFalseForAdminWithInvalidUuidSubject() {
        WalletRequestAccessPolicy policy = new WalletRequestAccessPolicy(JWT_SECRET);
        String jwt = generateJwtToken("not-a-uuid", "ADMIN");

        assertFalse(policy.isOwnerMismatchJwt("Bearer " + jwt, UUID.randomUUID()));
    }

    private String generateJwtToken(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
