package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WalletMutationStrategyResolverTest {

    @Test
    void resolve_ShouldReturnCreditStrategy_ForTopUp() {
        WalletMutationStrategyResolver resolver = new WalletMutationStrategyResolver();

        WalletMutationStrategy strategy = resolver.resolve(TransactionType.TOPUP);

        assertInstanceOf(CreditMutationStrategy.class, strategy);
    }

    @Test
    void resolve_ShouldReturnCreditStrategy_ForRefund() {
        WalletMutationStrategyResolver resolver = new WalletMutationStrategyResolver();

        WalletMutationStrategy strategy = resolver.resolve(TransactionType.REFUND);

        assertInstanceOf(CreditMutationStrategy.class, strategy);
    }

    @Test
    void resolve_ShouldReturnDebitStrategy_ForPayment() {
        WalletMutationStrategyResolver resolver = new WalletMutationStrategyResolver();

        WalletMutationStrategy strategy = resolver.resolve(TransactionType.PAYMENT);

        assertInstanceOf(DebitMutationStrategy.class, strategy);
    }

    @Test
    void resolve_ShouldReturnDebitStrategy_ForWithdraw() {
        WalletMutationStrategyResolver resolver = new WalletMutationStrategyResolver();

        WalletMutationStrategy strategy = resolver.resolve(TransactionType.WITHDRAW);

        assertInstanceOf(DebitMutationStrategy.class, strategy);
    }
}

