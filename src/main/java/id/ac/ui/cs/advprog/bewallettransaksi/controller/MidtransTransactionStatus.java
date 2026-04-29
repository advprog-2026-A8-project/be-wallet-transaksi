package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.Set;

public final class MidtransTransactionStatus {

    public static final String SETTLEMENT = "settlement";
    public static final Set<String> FAILURE_STATUSES = Set.of("deny", "cancel", "expire", "failure");

    private static final Set<String> SUPPORTED_STATUSES = Set.of(
            "capture", "settlement", "pending", "deny", "cancel", "expire", "failure"
    );

    private MidtransTransactionStatus() {
    }

    public static boolean isSupported(String status) {
        return SUPPORTED_STATUSES.contains(status);
    }

    public static boolean isSettlement(String status) {
        return SETTLEMENT.equalsIgnoreCase(status);
    }

    public static boolean isFailure(String status) {
        return status != null && FAILURE_STATUSES.contains(status.toLowerCase());
    }
}
