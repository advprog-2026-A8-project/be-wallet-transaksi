package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class NoopUsernameToUserIdResolver implements UsernameToUserIdResolver {
    @Override
    public Optional<UUID> resolve(String username) {
        return Optional.empty();
    }
}
