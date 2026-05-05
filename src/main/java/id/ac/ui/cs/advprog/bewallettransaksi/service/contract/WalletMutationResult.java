package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import java.math.BigDecimal;

public record WalletMutationResult(
        boolean success,
        BigDecimal updatedBalance,
        String errorCode,
        boolean retryable
) {
    public WalletMutationResult(boolean success, BigDecimal updatedBalance, String errorCode) {
        this(success, updatedBalance, errorCode, false);
    }

    public static WalletMutationResult success(BigDecimal updatedBalance) {
        return new WalletMutationResult(
                true,
                updatedBalance,
                WalletContractErrorCode.NONE.name(),
                false
        );
    }

    public static WalletMutationResult failure(WalletContractErrorCode errorCode) {
        return new WalletMutationResult(
                false,
                null,
                errorCode.name(),
                errorCode.isRetryable()
        );
    }
}
