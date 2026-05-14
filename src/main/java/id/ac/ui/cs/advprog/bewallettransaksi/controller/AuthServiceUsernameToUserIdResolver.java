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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthServiceUsernameToUserIdResolver implements UsernameToUserIdResolver {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceUsernameToUserIdResolver.class);
    private static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofMillis(1000);
    private static final Pattern USER_ID_CAMEL_PATTERN =
            uuidFieldPattern("userId", false);
    private static final Pattern ID_CAMEL_PATTERN =
            uuidFieldPattern("id", false);
    private static final List<Pattern> USER_ID_PATTERNS = List.of(
            USER_ID_CAMEL_PATTERN,
            ID_CAMEL_PATTERN
    );

    private final String authServiceBaseUrl;
    private final String userLookupPath;
    private final HttpClient httpClient;
    private final Duration httpTimeout;

    public AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl) {
        this(authServiceBaseUrl, UsernameToUserIdResolverConfig.DEFAULT_USER_LOOKUP_PATH, DEFAULT_HTTP_TIMEOUT);
    }

    AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl, String userLookupPath, Duration httpTimeout) {
        this(authServiceBaseUrl, userLookupPath, createHttpClient(httpTimeout), httpTimeout);
    }

    AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl, HttpClient httpClient) {
        this(authServiceBaseUrl, UsernameToUserIdResolverConfig.DEFAULT_USER_LOOKUP_PATH, httpClient, DEFAULT_HTTP_TIMEOUT);
    }

    AuthServiceUsernameToUserIdResolver(String authServiceBaseUrl, HttpClient httpClient, Duration httpTimeout) {
        this(authServiceBaseUrl, UsernameToUserIdResolverConfig.DEFAULT_USER_LOOKUP_PATH, httpClient, httpTimeout);
    }

    AuthServiceUsernameToUserIdResolver(
            String authServiceBaseUrl,
            String userLookupPath,
            HttpClient httpClient,
            Duration httpTimeout
    ) {
        this.authServiceBaseUrl = normalizeBaseUrl(authServiceBaseUrl);
        this.userLookupPath = normalizeLookupPath(userLookupPath);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.httpTimeout = normalizeTimeout(httpTimeout);
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
            log.warn("auth.username.lookup.interrupted");
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("auth.username.lookup.failed error={}", ex.toString());
            return Optional.empty();
        }
    }

    private URI buildUserLookupUri(String username) {
        String encoded = encodeQueryParam(username);
        return URI.create(authServiceBaseUrl + userLookupPath + "?email=" + encoded);
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
            return UsernameToUserIdResolverConfig.DEFAULT_USER_LOOKUP_PATH;
        }
        String normalized = lookupPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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
