package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceUsernameToUserIdResolverTest {

    @Test
    void resolve_WhenUsernameNullOrBlank_ShouldReturnEmpty() {
        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://auth-service");

        assertTrue(resolver.resolve(null).isEmpty());
        assertTrue(resolver.resolve("   ").isEmpty());
    }

    @Test
    void resolve_WhenNoAuthServiceResponse_ShouldReturnEmpty() {
        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://auth-service");

        Optional<UUID> resolved = resolver.resolve("owner_username");

        assertTrue(resolved.isEmpty());
    }

    @Test
    void resolve_ShouldFetchUserIdFromAuthServiceEndpoint() throws Exception {
        UUID expectedUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        HttpServer server = createServerWithResponse("{\"userId\":\"" + expectedUserId + "\"}");
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("api_user");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_ShouldExtractUuidFromApiResponseDataId() throws Exception {
        UUID expectedUserId = UUID.fromString("21212121-2121-2121-2121-212121212121");
        String body = "{\"message\":\"ok\",\"data\":{\"id\":\"" + expectedUserId + "\"}}";
        HttpServer server = createServerWithResponse(body);
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("api_user");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenApiResponseDataIdIsLong_ShouldReturnEmpty() throws Exception {
        String body = "{\"message\":\"ok\",\"data\":{\"id\":123}}";
        HttpServer server = createServerWithResponse(body);
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("api_user");

            assertTrue(resolved.isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_ShouldEncodeSpaceInUsernameAsPercent20() throws Exception {
        UUID expectedUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/profile/lookup", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String response = query != null && query.contains("email=owner%20name")
                    ? "{\"userId\":\"" + expectedUserId + "\"}"
                    : "{}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("owner name");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceReturnsInvalidUserIdFormat_ShouldReturnEmpty() throws Exception {
        HttpServer server = createServerWithResponse("{\"userId\":\"not-a-uuid\"}");
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("broken_user");

            assertTrue(resolved.isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceUnavailable_ShouldReturnEmpty() {
        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://localhost:1");

        Optional<UUID> resolved = resolver.resolve("owner_username");

        assertTrue(resolved.isEmpty());
    }

    @Test
    void resolve_WhenAuthServiceIsSlow_ShouldFailFastAndReturnEmpty() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        CountDownLatch slowResponseGate = new CountDownLatch(1);
        server.createContext("/api/profile/lookup", exchange -> {
            try {
                slowResponseGate.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            String response = "{\"userId\":\"44444444-4444-4444-4444-444444444444\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = assertTimeoutPreemptively(
                    Duration.ofMillis(1200),
                    () -> resolver.resolve("slow_user")
            );
            assertTrue(resolved.isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WithTrailingSlashBaseUrl_ShouldStillResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        HttpServer server = createServerWithResponse("{\"userId\":\"" + expectedUserId + "\"}");
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/";
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WithWhitespaceAroundBaseUrl_ShouldStillResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        HttpServer server = createServerWithResponse("{\"userId\":\"" + expectedUserId + "\"}");
        server.start();
        try {
            String baseUrl = "  http://localhost:" + server.getAddress().getPort() + "/  ";
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenTimeoutConfigIsNull_ShouldFallbackToDefaultTimeout() throws Exception {
        UUID expectedUserId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        HttpServer server = createServerWithResponse("{\"userId\":\"" + expectedUserId + "\"}");
        server.start();
        try (ServerStopper ignored = new ServerStopper(server)) {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl, HttpClient.newHttpClient(), null);

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        }
    }

    @Test
    void resolve_WhenTimeoutConfigIsZero_ShouldFallbackToDefaultTimeout() throws Exception {
        UUID expectedUserId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        HttpServer server = createServerWithResponse("{\"userId\":\"" + expectedUserId + "\"}");
        server.start();
        try (ServerStopper ignored = new ServerStopper(server)) {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(baseUrl, HttpClient.newHttpClient(), Duration.ZERO);

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        }
    }

    @Test
    void resolve_WithCustomLookupPathWithoutLeadingSlash_ShouldNormalizePath() throws Exception {
        UUID expectedUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/custom/lookup", exchange -> {
            String response = "{\"userId\":\"" + expectedUserId + "\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            AuthServiceUsernameToUserIdResolver resolver =
                    new AuthServiceUsernameToUserIdResolver(
                            baseUrl,
                            "api/custom/lookup/",
                            HttpClient.newHttpClient(),
                            Duration.ofMillis(1000),
                            null
                    );

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceStatusNot200_ShouldReturnEmpty() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(404);
        when(response.body()).thenReturn("{\"userId\":\"11111111-1111-1111-1111-111111111111\"}");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://auth-service", httpClient, Duration.ofMillis(500));

        Optional<UUID> resolved = resolver.resolve("user@example.com");

        assertTrue(resolved.isEmpty());
    }

    @Test
    void resolve_WhenBodyNull_ShouldReturnEmpty() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(null);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://auth-service", httpClient, Duration.ofMillis(500));

        assertTrue(resolver.resolve("user@example.com").isEmpty());
    }

    @Test
    void resolve_WhenInterrupted_ShouldReturnEmptyAndPreserveInterruptFlag() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));

        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://auth-service", httpClient, Duration.ofMillis(500));

        try {
            Optional<UUID> resolved = resolver.resolve("user@example.com");
            assertTrue(resolved.isEmpty());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private static final class ServerStopper implements AutoCloseable {
        private final HttpServer server;

        private ServerStopper(HttpServer server) {
            this.server = server;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private HttpServer createServerWithResponse(String response) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/profile/lookup", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        return server;
    }
}
