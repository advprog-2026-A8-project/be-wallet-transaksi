package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthServiceUsernameToUserIdResolver implements UsernameToUserIdResolver {
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("\"userId\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

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
        return normalize(username)
                .flatMap(this::resolveWithFallback);
    }

    String getAuthServiceBaseUrl() {
        return authServiceBaseUrl;
    }

    private Optional<String> normalize(String username) {
        if (username == null) {
            return Optional.empty();
        }
        String normalized = username.trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }

    private Optional<UUID> resolveWithFallback(String username) {
        return resolveFromAuthService(username);
    }

    private Optional<UUID> resolveFromAuthService(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUserLookupUri(username))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return Optional.empty();
            }
            return extractUserId(response.body());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private URI buildUserLookupUri(String username) {
        String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return URI.create(authServiceBaseUrl + "/internal/users/by-username?username=" + encoded);
    }

    private Optional<UUID> extractUserId(String responseBody) {
        Matcher matcher = USER_ID_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(matcher.group(1)));
    }
}
