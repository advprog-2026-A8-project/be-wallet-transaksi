package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletMutationResultTest {

    @Test
    void successFactory_ShouldBuildSuccessfulResult() {
        WalletMutationResult result = WalletMutationResult.success(new BigDecimal("10.00"));

        assertTrue(result.success());
        assertEquals(new BigDecimal("10.00"), result.updatedBalance());
        assertEquals(WalletContractErrorCode.NONE.name(), result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    void failureFactory_ShouldFollowErrorRetryability() {
        WalletMutationResult nonRetryable = WalletMutationResult.failure(WalletContractErrorCode.INVALID_REQUEST);
        WalletMutationResult retryable = WalletMutationResult.failure(WalletContractErrorCode.INTERNAL_ERROR);

        assertFalse(nonRetryable.success());
        assertNull(nonRetryable.updatedBalance());
        assertFalse(nonRetryable.retryable());

        assertFalse(retryable.success());
        assertNull(retryable.updatedBalance());
        assertTrue(retryable.retryable());
    }
}

