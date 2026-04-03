package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import java.math.BigDecimal;

public class CreditMutationStrategy implements WalletMutationStrategy {

    @Override
    public BigDecimal apply(BigDecimal balance, BigDecimal amount) {
        return balance.add(amount);
    }
}

