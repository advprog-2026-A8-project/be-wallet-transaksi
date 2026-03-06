package id.ac.ui.cs.advprog.bewallettransaksi.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID userId) {
        super("Wallet not found for user: " + userId);
    }
}
