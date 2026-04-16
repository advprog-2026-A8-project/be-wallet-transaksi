package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import java.math.BigDecimal;

public interface WalletMutationStrategy {
    BigDecimal apply(BigDecimal balance, BigDecimal amount);
}

