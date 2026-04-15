package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;

public final class TransactionStateFactory {
    private static final TransactionState PENDING_STATE = new PendingState();
    private static final TransactionState SUCCESS_STATE = new SuccessState();
    private static final TransactionState FAILED_STATE = new FailedState();

    private TransactionStateFactory() {
    }

    public static TransactionState from(TransactionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Transaction status must not be null");
        }

        return switch (status) {
            case PENDING -> PENDING_STATE;
            case SUCCESS -> SUCCESS_STATE;
            case FAILED -> FAILED_STATE;
        };
    }
}
