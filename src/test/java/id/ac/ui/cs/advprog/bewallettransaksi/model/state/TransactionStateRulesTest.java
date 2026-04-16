package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionStateRulesTest {

    @Test
    void pendingState_ShouldAllowPendingSuccessAndFailed() {
        PendingState state = new PendingState();

        assertTrue(state.canTransitionTo(TransactionStatus.PENDING));
        assertTrue(state.canTransitionTo(TransactionStatus.SUCCESS));
        assertTrue(state.canTransitionTo(TransactionStatus.FAILED));
    }

    @Test
    void pendingState_ShouldRejectNullStatus() {
        PendingState state = new PendingState();

        assertFalse(state.canTransitionTo(null));
    }

    @Test
    void successState_ShouldOnlyAllowSuccess() {
        SuccessState state = new SuccessState();

        assertTrue(state.canTransitionTo(TransactionStatus.SUCCESS));
        assertFalse(state.canTransitionTo(TransactionStatus.PENDING));
        assertFalse(state.canTransitionTo(TransactionStatus.FAILED));
        assertFalse(state.canTransitionTo(null));
    }

    @Test
    void failedState_ShouldOnlyAllowFailed() {
        FailedState state = new FailedState();

        assertTrue(state.canTransitionTo(TransactionStatus.FAILED));
        assertFalse(state.canTransitionTo(TransactionStatus.PENDING));
        assertFalse(state.canTransitionTo(TransactionStatus.SUCCESS));
        assertFalse(state.canTransitionTo(null));
    }
}
