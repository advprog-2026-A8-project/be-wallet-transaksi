package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;

public class FailedState implements TransactionState {
    @Override
    public boolean canTransitionTo(TransactionStatus nextStatus) {
        return nextStatus == TransactionStatus.FAILED;
    }
}
