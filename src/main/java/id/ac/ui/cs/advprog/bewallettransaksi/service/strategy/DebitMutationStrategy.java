package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import java.math.BigDecimal;

public class DebitMutationStrategy implements WalletMutationStrategy {

    @Override
    public BigDecimal apply(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        return balance.subtract(amount);
    }
}

