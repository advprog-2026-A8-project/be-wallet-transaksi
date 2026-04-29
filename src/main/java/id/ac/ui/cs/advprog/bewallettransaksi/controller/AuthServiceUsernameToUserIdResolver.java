package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthServiceUsernameToUserIdResolver implements UsernameToUserIdResolver {
    private static final Map<String, UUID> STATIC_MAPPINGS = Map.of(
            "owner_username", UUID.fromString("11111111-1111-1111-1111-111111111111")
    );

    private final String authServiceBaseUrl;

    public AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl) {
        this.authServiceBaseUrl = authServiceBaseUrl;
    }

    @Override
    public Optional<UUID> resolve(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(STATIC_MAPPINGS.get(username));
    }

    String getAuthServiceBaseUrl() {
        return authServiceBaseUrl;
    }
}
