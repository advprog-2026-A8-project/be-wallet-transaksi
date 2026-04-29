package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.Set;

public final class MidtransTransactionStatus {

    private static final Set<String> SUPPORTED_STATUSES = Set.of(
            "capture", "settlement", "pending", "deny", "cancel", "expire", "failure"
    );

    private MidtransTransactionStatus() {
    }

    public static boolean isSupported(String status) {
        return SUPPORTED_STATUSES.contains(status);
    }
}
