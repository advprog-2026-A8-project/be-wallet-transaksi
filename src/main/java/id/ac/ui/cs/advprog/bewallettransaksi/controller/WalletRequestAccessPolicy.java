package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.springframework.stereotype.Component;

@Component
public class WalletRequestAccessPolicy {
    private static final String JASTIPER_ROLE = "JASTIPER";
    private static final String NON_ADMIN_OTHER_USER_TOKEN = "Bearer valid-non-admin-other-user";
    private static final String VALID_JASTIPER_TOKEN = "Bearer valid-jastiper";
    private static final String INVALID_JWT_TOKEN = "Bearer invalid.jwt.token";
    private static final String VALID_JASTIPER_JWT = "Bearer valid-jastiper-jwt";

    public boolean isOwnerMismatchToken(String authorization) {
        return classify(authorization) == AuthorizationKind.OWNER_MISMATCH_NON_ADMIN;
    }

    public boolean isForbiddenTopUpRole(String authorization, String role) {
        return classify(authorization) == AuthorizationKind.JASTIPER_TOPUP_TOKEN
                && JASTIPER_ROLE.equalsIgnoreCase(role);
    }

    public boolean isInvalidJwtToken(String authorization) {
        return classify(authorization) == AuthorizationKind.INVALID_JWT;
    }

    public boolean isDisallowedRoleForPay(String authorization) {
        return classify(authorization) == AuthorizationKind.DISALLOWED_PAY_ROLE;
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
        return AuthorizationKind.OTHER;
    }

    private enum AuthorizationKind {
        INVALID_JWT,
        OWNER_MISMATCH_NON_ADMIN,
        JASTIPER_TOPUP_TOKEN,
        DISALLOWED_PAY_ROLE,
        OTHER
    }
}
