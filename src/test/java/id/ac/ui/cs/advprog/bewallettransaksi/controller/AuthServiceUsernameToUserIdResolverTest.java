package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void resolve_WhenAuthServiceUnavailable_ShouldReturnEmpty() {
        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://localhost:1");

        Optional<UUID> resolved = resolver.resolve("owner_username");

        assertTrue(resolved.isEmpty());
    }
}
