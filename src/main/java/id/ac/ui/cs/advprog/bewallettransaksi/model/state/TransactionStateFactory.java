package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;

public final class TransactionStateFactory {
    private TransactionStateFactory() {
    }

    public static TransactionState from(TransactionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Transaction status must not be null");
        }

        return switch (status) {
            case PENDING -> new PendingState();
            case SUCCESS -> new SuccessState();
            case FAILED -> new FailedState();
        };
    }
}
