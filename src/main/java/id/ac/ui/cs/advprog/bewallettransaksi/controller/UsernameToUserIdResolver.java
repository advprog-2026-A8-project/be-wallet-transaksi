package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.Optional;
import java.util.UUID;

public interface UsernameToUserIdResolver {
    Optional<UUID> resolve(String username);
}
