package id.ac.ui.cs.advprog.bewallettransaksi.model.state;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionStateFactoryTest {

    @Test
    void fromPending_ShouldReturnPendingState() {
        TransactionState state = TransactionStateFactory.from(TransactionStatus.PENDING);
        assertInstanceOf(PendingState.class, state);
    }

    @Test
    void fromSuccess_ShouldReturnSuccessState() {
        TransactionState state = TransactionStateFactory.from(TransactionStatus.SUCCESS);
        assertInstanceOf(SuccessState.class, state);
    }

    @Test
    void fromFailed_ShouldReturnFailedState() {
        TransactionState state = TransactionStateFactory.from(TransactionStatus.FAILED);
        assertInstanceOf(FailedState.class, state);
    }

    @Test
    void fromNullStatus_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> TransactionStateFactory.from(null));
    }
}
