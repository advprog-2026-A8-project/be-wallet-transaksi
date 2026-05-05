package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

public enum WalletContractErrorCode {
    NONE(false),
    WALLET_NOT_FOUND(false),
    INSUFFICIENT_BALANCE(false),
    PAYMENT_NOT_FOUND(false),
    INVALID_REQUEST(false),
    INTERNAL_ERROR(true);

    private final boolean retryable;

    WalletContractErrorCode(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
