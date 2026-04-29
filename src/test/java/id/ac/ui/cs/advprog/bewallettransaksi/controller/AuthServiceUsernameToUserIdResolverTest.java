package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceUsernameToUserIdResolverTest {

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
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
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
                    new AuthServiceUsernameToUserIdResolver(baseUrl);

            Optional<UUID> resolved = resolver.resolve("api_user");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_ShouldEncodeSpaceInUsernameAsPercent20() throws Exception {
        UUID expectedUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String response = query != null && query.contains("username=owner%20name")
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
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "{\"userId\":\"not-a-uuid\"}";
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
        server.createContext("/internal/users/by-username", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
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
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "{\"userId\":\"" + expectedUserId + "\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
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
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "{\"userId\":\"" + expectedUserId + "\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
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
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
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
                    new AuthServiceUsernameToUserIdResolver(baseUrl, HttpClient.newHttpClient(), null);

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenTimeoutConfigIsZero_ShouldFallbackToDefaultTimeout() throws Exception {
        UUID expectedUserId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
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
                    new AuthServiceUsernameToUserIdResolver(baseUrl, HttpClient.newHttpClient(), Duration.ZERO);

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceReturnsIdField_ShouldResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "{\"id\":\"" + expectedUserId + "\"}";
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

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceReturnsNestedDataUserId_ShouldResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "{\"data\":{\"userId\":\"" + expectedUserId + "\"}}";
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

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceReturnsRawUuidBody_ShouldResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = expectedUserId.toString();
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
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

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceReturnsQuotedUuidBody_ShouldResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "\"" + expectedUserId + "\"";
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

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceReturnsQuotedUuidWithNewline_ShouldResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "\"\n" + expectedUserId + "\n\"";
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

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolve_WhenAuthServiceReturnsUserIDFieldVariant_ShouldResolveUserId() throws Exception {
        UUID expectedUserId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/users/by-username", exchange -> {
            String response = "{\"userID\":\"" + expectedUserId + "\"}";
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

            Optional<UUID> resolved = resolver.resolve("owner_username");

            assertTrue(resolved.isPresent());
            assertEquals(expectedUserId, resolved.get());
        } finally {
            server.stop(0);
        }
    }
}
