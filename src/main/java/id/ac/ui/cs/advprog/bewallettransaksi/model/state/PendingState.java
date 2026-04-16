package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;

public class PendingState implements TransactionState {
    @Override
    public boolean canTransitionTo(TransactionStatus nextStatus) {
        return nextStatus == TransactionStatus.PENDING
                || nextStatus == TransactionStatus.SUCCESS
                || nextStatus == TransactionStatus.FAILED;
    }
}
