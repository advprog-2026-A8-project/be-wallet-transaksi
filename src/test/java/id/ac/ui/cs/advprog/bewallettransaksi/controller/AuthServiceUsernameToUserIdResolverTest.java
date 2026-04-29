package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceUsernameToUserIdResolverTest {

    @Test
    void resolve_ExistingUsername_ShouldReturnUserId() {
        AuthServiceUsernameToUserIdResolver resolver =
                new AuthServiceUsernameToUserIdResolver("http://auth-service");

        Optional<UUID> resolved = resolver.resolve("owner_username");

        assertTrue(resolved.isPresent());
        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), resolved.get());
    }
}
