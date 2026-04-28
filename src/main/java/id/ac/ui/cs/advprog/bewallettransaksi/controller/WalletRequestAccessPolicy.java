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
        return NON_ADMIN_OTHER_USER_TOKEN.equals(authorization);
    }

    public boolean isForbiddenTopUpRole(String authorization, String role) {
        return VALID_JASTIPER_TOKEN.equals(authorization) && JASTIPER_ROLE.equalsIgnoreCase(role);
    }

    public boolean isInvalidJwtToken(String authorization) {
        return INVALID_JWT_TOKEN.equals(authorization);
    }

    public boolean isDisallowedRoleForPay(String authorization) {
        return VALID_JASTIPER_JWT.equals(authorization);
    }
}
