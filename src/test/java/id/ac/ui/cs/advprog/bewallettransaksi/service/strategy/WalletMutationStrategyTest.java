package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletMutationStrategyTest {

    @Test
    void creditMutationStrategy_ShouldAddAmount() {
        WalletMutationStrategy strategy = new CreditMutationStrategy();

        BigDecimal result = strategy.apply(BigDecimal.valueOf(100), BigDecimal.valueOf(25));

        assertEquals(0, BigDecimal.valueOf(125).compareTo(result));
    }

    @Test
    void debitMutationStrategy_ShouldSubtractAmount_WhenBalanceSufficient() {
        WalletMutationStrategy strategy = new DebitMutationStrategy();

        BigDecimal result = strategy.apply(BigDecimal.valueOf(100), BigDecimal.valueOf(40));

        assertEquals(0, BigDecimal.valueOf(60).compareTo(result));
    }

    @Test
    void debitMutationStrategy_ShouldThrow_WhenBalanceInsufficient() {
        WalletMutationStrategy strategy = new DebitMutationStrategy();
        BigDecimal balance = BigDecimal.valueOf(50);
        BigDecimal amount = BigDecimal.valueOf(60);

        assertThrows(IllegalStateException.class,
                () -> strategy.apply(balance, amount));
    }
}
