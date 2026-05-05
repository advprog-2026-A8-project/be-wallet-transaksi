package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthServiceUsernameToUserIdResolver implements UsernameToUserIdResolver {
    private static final String DEFAULT_USER_LOOKUP_PATH = "/internal/users/by-username";
    private static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofMillis(1000);
    private static final Pattern USER_ID_CAMEL_PATTERN =
            uuidFieldPattern("userId", false);
    private static final List<Pattern> USER_ID_PATTERNS = List.of(
            USER_ID_CAMEL_PATTERN
    );

    private final String authServiceBaseUrl;
    private final HttpClient httpClient;
    private final Duration httpTimeout;
    private final String userLookupPath;

    public AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl) {
        this(authServiceBaseUrl, DEFAULT_HTTP_TIMEOUT);
    }

    AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl, Duration httpTimeout) {
        this(authServiceBaseUrl, createHttpClient(httpTimeout), httpTimeout, DEFAULT_USER_LOOKUP_PATH);
    }

    AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl, HttpClient httpClient) {
        this(authServiceBaseUrl, httpClient, DEFAULT_HTTP_TIMEOUT, DEFAULT_USER_LOOKUP_PATH);
    }

    AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl, HttpClient httpClient, Duration httpTimeout) {
        this(authServiceBaseUrl, httpClient, httpTimeout, DEFAULT_USER_LOOKUP_PATH);
    }

    AuthServiceUsernameToUserIdResolver(
            String authServiceBaseUrl,
            HttpClient httpClient,
            Duration httpTimeout,
            String userLookupPath
    ) {
        this.authServiceBaseUrl = normalizeBaseUrl(authServiceBaseUrl);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.httpTimeout = normalizeTimeout(httpTimeout);
        this.userLookupPath = normalizeLookupPath(userLookupPath);
    }

    private static HttpClient createHttpClient(Duration httpTimeout) {
        return HttpClient.newBuilder()
                .connectTimeout(httpTimeout)
                .build();
    }

    private static Pattern uuidFieldPattern(String fieldName, boolean caseInsensitive) {
        String prefix = caseInsensitive ? "(?i)" : "";
        return Pattern.compile(prefix + "\"" + fieldName + "\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");
    }

    @Override
    public Optional<UUID> resolve(String username) {
        return normalize(username)
                .flatMap(this::resolveFromAuthService);
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

    private Optional<UUID> resolveFromAuthService(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUserLookupUri(username))
                    .timeout(httpTimeout)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return Optional.empty();
            }
            return extractUserId(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private URI buildUserLookupUri(String username) {
        String encoded = encodeQueryParam(username);
        return URI.create(authServiceBaseUrl + userLookupPath + "?username=" + encoded);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static Duration normalizeTimeout(Duration timeout) {
        if (!isPositive(timeout)) {
            return DEFAULT_HTTP_TIMEOUT;
        }
        return timeout;
    }

    private static String normalizeLookupPath(String lookupPath) {
        if (lookupPath == null || lookupPath.isBlank()) {
            return DEFAULT_USER_LOOKUP_PATH;
        }
        String normalized = lookupPath.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static boolean isPositive(Duration timeout) {
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }

    private String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private Optional<UUID> extractUserId(String responseBody) {
        return USER_ID_PATTERNS.stream()
                .map(pattern -> extractUuidWithPattern(responseBody, pattern))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Optional<UUID> extractUuidWithPattern(String responseBody, Pattern pattern) {
        Matcher matcher = pattern.matcher(responseBody);
        if (matcher.find()) {
            return Optional.of(UUID.fromString(matcher.group(1)));
        }
        return Optional.empty();
    }

}
