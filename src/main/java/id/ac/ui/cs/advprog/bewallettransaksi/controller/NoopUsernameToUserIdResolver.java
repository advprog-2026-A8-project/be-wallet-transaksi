package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.Optional;
import java.util.UUID;

public class NoopUsernameToUserIdResolver implements UsernameToUserIdResolver {
    @Override
    public Optional<UUID> resolve(String username) {
        return Optional.empty();
    }
}
