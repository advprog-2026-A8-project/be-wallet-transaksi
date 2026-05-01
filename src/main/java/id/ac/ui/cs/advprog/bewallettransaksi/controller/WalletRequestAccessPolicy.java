package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class WalletRequestAccessPolicy {
    private static final String JASTIPER_ROLE = "JASTIPER";
    private static final String TITIPER_ROLE = "TITIPER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ROLE_CLAIM = "role";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String jwtSecret;
    private final UsernameToUserIdResolver usernameToUserIdResolver;

    public WalletRequestAccessPolicy(
            @Value("${jwt.secret:DefaultSecretKeyUntukDevelopmentLokalYangSangatPanjangSekali123!@#}") String jwtSecret,
            UsernameToUserIdResolver usernameToUserIdResolver
    ) {
        this.jwtSecret = jwtSecret;
        this.usernameToUserIdResolver = usernameToUserIdResolver;
    }

    public boolean isOwnerMismatchToken() {
        return false;
    }

    public boolean isOwnerMismatchJwt(String authorization, UUID targetUserId) {
        if (!isJwtToken(authorization) || targetUserId == null) {
            return false;
        }
        Claims claims = parseClaims(authorization);
        if (claims == null) {
            return false;
        }
        if (isAdmin(claims)) {
            return false;
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            return true;
        }
        UUID subjectUserId = resolveSubjectUserId(subject).orElse(null);
        return subjectUserId == null || !subjectUserId.equals(targetUserId);
    }

    public boolean isForbiddenTopUpRole(String authorization) {
        if (isJwtToken(authorization)) {
            return isValidSignedJwtWithRole(authorization, JASTIPER_ROLE);
        }
        return false;
    }

    public boolean isInvalidJwtToken(String authorization) {
        if (!hasBearerPrefix(authorization)) {
            return false;
        }
        return isJwtToken(authorization) && !isJwtParsable(authorization);
    }

    public boolean isDisallowedRoleForPay(String authorization) {
        return isValidSignedJwtWithRole(authorization, JASTIPER_ROLE);
    }

    public boolean isValidReadJwt(String authorization) {
        return isJwtToken(authorization) && isJwtParsable(authorization);
    }

    public boolean isValidJastiperJwt(String authorization) {
        return isValidSignedJwtWithRole(authorization, JASTIPER_ROLE);
    }

    public boolean isValidTitiperJwt(String authorization) {
        return isValidSignedJwtWithRole(authorization, TITIPER_ROLE);
    }

    public boolean isValidAdminJwt(String authorization) {
        return isValidSignedJwtWithRole(authorization, ADMIN_ROLE);
    }

    public boolean isAllowedPayRole(String authorization) {
        return isValidTitiperJwt(authorization) || isValidAdminJwt(authorization);
    }

    public boolean isAllowedWalletMutationRole(String authorization) {
        if (isJwtToken(authorization)) {
            Claims claims = parseClaims(authorization);
            if (claims == null) {
                return false;
            }
            String role = claims.get(ROLE_CLAIM, String.class);
            return isSupportedMutationRole(role);
        }
        return false;
    }

    public boolean isJwtBearerToken(String authorization) {
        return isJwtToken(authorization);
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

    private boolean isAdmin(Claims claims) {
        String role = claims.get(ROLE_CLAIM, String.class);
        return role != null && ADMIN_ROLE.equalsIgnoreCase(role);
    }

    private boolean isSupportedMutationRole(String role) {
        return role != null
                && (ADMIN_ROLE.equalsIgnoreCase(role)
                || TITIPER_ROLE.equalsIgnoreCase(role)
                || JASTIPER_ROLE.equalsIgnoreCase(role));
    }

    private Optional<UUID> resolveSubjectUserId(String subject) {
        try {
            return Optional.of(UUID.fromString(subject));
        } catch (IllegalArgumentException ex) {
            return usernameToUserIdResolver.resolve(subject);
        }
    }

}
