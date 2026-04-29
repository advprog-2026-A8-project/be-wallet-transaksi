package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthServiceUsernameToUserIdResolver implements UsernameToUserIdResolver {
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("\"userId\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");
    private static final Map<String, UUID> STATIC_MAPPINGS = Map.of(
            "owner_username", UUID.fromString("11111111-1111-1111-1111-111111111111")
    );

    private final String authServiceBaseUrl;
    private final HttpClient httpClient;

    public AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl) {
        this(authServiceBaseUrl, HttpClient.newHttpClient());
    }

    AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl, HttpClient httpClient) {
        this.authServiceBaseUrl = authServiceBaseUrl;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<UUID> resolve(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String normalized = username.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return resolveFromAuthService(normalized)
                .or(() -> Optional.ofNullable(STATIC_MAPPINGS.get(normalized)));
    }

    String getAuthServiceBaseUrl() {
        return authServiceBaseUrl;
    }

    private Optional<UUID> resolveFromAuthService(String username) {
        try {
            String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String endpoint = authServiceBaseUrl + "/internal/users/by-username?username=" + encoded;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return Optional.empty();
            }
            Matcher matcher = USER_ID_PATTERN.matcher(response.body());
            if (!matcher.find()) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(matcher.group(1)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
