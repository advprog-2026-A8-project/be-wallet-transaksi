package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoopUsernameToUserIdResolverTest {

    @Test
    void resolve_ShouldAlwaysReturnEmpty() {
        NoopUsernameToUserIdResolver resolver = new NoopUsernameToUserIdResolver();

        Optional<UUID> resolved = resolver.resolve("someone@example.com");

        assertTrue(resolved.isEmpty());
        assertEquals(Optional.empty(), resolved);
    }
}

