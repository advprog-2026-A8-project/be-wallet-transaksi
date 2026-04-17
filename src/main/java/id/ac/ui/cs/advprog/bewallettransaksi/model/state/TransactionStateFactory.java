package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;

import java.util.Map;

public final class TransactionStateFactory {
    private static final Map<TransactionStatus, TransactionState> STATE_MAP = Map.of(
            TransactionStatus.PENDING, new PendingState(),
            TransactionStatus.SUCCESS, new SuccessState(),
            TransactionStatus.FAILED, new FailedState()
    );

    private TransactionStateFactory() {
    }

    public static TransactionState from(TransactionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Transaction status must not be null");
        }

        TransactionState state = STATE_MAP.get(status);
        if (state == null) {
            throw new IllegalArgumentException("Unsupported transaction status: " + status);
        }
        return state;
    }
}
