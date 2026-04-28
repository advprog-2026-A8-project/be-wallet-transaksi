package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class WalletRequestAccessPolicy {
    private static final String JASTIPER_ROLE = "JASTIPER";
    private static final String TITIPER_ROLE = "TITIPER";
    private static final String ROLE_CLAIM = "role";
    private static final String NON_ADMIN_OTHER_USER_TOKEN = "Bearer valid-non-admin-other-user";
    private static final String VALID_JASTIPER_TOKEN = "Bearer valid-jastiper";
    private static final String INVALID_JWT_TOKEN = "Bearer invalid.jwt.token";
    private static final String VALID_JASTIPER_JWT = "Bearer valid-jastiper-jwt";
    private static final String VALID_READ_JWT = "Bearer valid-read-jwt";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String jwtSecret;

    public WalletRequestAccessPolicy(
            @Value("${jwt.secret:DefaultSecretKeyUntukDevelopmentLokalYangSangatPanjangSekali123!@#}") String jwtSecret
    ) {
        this.jwtSecret = jwtSecret;
    }

    public boolean isOwnerMismatchToken(String authorization) {
        if (isJwtToken(authorization)) {
            return false;
        }
        return classify(authorization) == AuthorizationKind.OWNER_MISMATCH_NON_ADMIN;
    }

    public boolean isForbiddenTopUpRole(String authorization, String role) {
        if (isJwtToken(authorization)) {
            if (isValidSignedJwtWithRole(authorization, JASTIPER_ROLE)) {
                return true;
            }
            return JASTIPER_ROLE.equalsIgnoreCase(role);
        }
        return classify(authorization) == AuthorizationKind.JASTIPER_TOPUP_TOKEN
                && JASTIPER_ROLE.equalsIgnoreCase(role);
    }

    public boolean isInvalidJwtToken(String authorization) {
        if (!hasBearerPrefix(authorization)) {
            return false;
        }
        if (isJwtToken(authorization)) {
            return !isJwtParsable(authorization);
        }
        return classify(authorization) == AuthorizationKind.INVALID_JWT;
    }

    public boolean isDisallowedRoleForPay(String authorization) {
        if (isValidSignedJwtWithRole(authorization, JASTIPER_ROLE)) {
            return true;
        }
        return classify(authorization) == AuthorizationKind.DISALLOWED_PAY_ROLE;
    }

    public boolean isValidReadJwt(String authorization) {
        if (isJwtToken(authorization)) {
            return isJwtParsable(authorization);
        }
        return classify(authorization) == AuthorizationKind.VALID_READ_JWT;
    }

    public boolean isValidJastiperJwt(String authorization) {
        if (isValidSignedJwtWithRole(authorization, JASTIPER_ROLE)) {
            return true;
        }
        return classify(authorization) == AuthorizationKind.DISALLOWED_PAY_ROLE;
    }

    public boolean isValidTitiperJwt(String authorization) {
        return isValidSignedJwtWithRole(authorization, TITIPER_ROLE);
    }

    private AuthorizationKind classify(String authorization) {
        if (INVALID_JWT_TOKEN.equals(authorization)) {
            return AuthorizationKind.INVALID_JWT;
        }
        if (NON_ADMIN_OTHER_USER_TOKEN.equals(authorization)) {
            return AuthorizationKind.OWNER_MISMATCH_NON_ADMIN;
        }
        if (VALID_JASTIPER_TOKEN.equals(authorization)) {
            return AuthorizationKind.JASTIPER_TOPUP_TOKEN;
        }
        if (VALID_JASTIPER_JWT.equals(authorization)) {
            return AuthorizationKind.DISALLOWED_PAY_ROLE;
        }
        if (VALID_READ_JWT.equals(authorization)) {
            return AuthorizationKind.VALID_READ_JWT;
        }
        return AuthorizationKind.OTHER;
    }

    private boolean isValidSignedJwtWithRole(String authorization, String expectedRole) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        Claims claims = parseClaims(authorization);
        if (claims == null) {
            return false;
        }
        String role = claims.get(ROLE_CLAIM, String.class);
        return expectedRole.equalsIgnoreCase(role);
    }

    private boolean isJwtParsable(String authorization) {
        return parseClaims(authorization) != null;
    }

    private boolean isJwtToken(String authorization) {
        if (!hasBearerPrefix(authorization)) {
            return false;
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        return token.chars().filter(ch -> ch == '.').count() == 2;
    }

    private Claims parseClaims(String authorization) {
        String token = authorization.substring(BEARER_PREFIX.length());
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean hasBearerPrefix(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX);
    }

    private enum AuthorizationKind {
        INVALID_JWT,
        OWNER_MISMATCH_NON_ADMIN,
        JASTIPER_TOPUP_TOKEN,
        DISALLOWED_PAY_ROLE,
        VALID_READ_JWT,
        OTHER
    }
}
