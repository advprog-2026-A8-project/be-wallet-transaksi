package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;

public interface TransactionState {
    boolean canTransitionTo(TransactionStatus nextStatus);
}
