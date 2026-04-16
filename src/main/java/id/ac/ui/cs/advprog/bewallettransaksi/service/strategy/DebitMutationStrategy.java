package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import java.math.BigDecimal;

public final class DebitMutationStrategy implements WalletMutationStrategy {

    private static final String INSUFFICIENT_BALANCE_MESSAGE = "Insufficient balance";

    @Override
    public BigDecimal apply(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException(INSUFFICIENT_BALANCE_MESSAGE);
        }
        return balance.subtract(amount);
    }
}
