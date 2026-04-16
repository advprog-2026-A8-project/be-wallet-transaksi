package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import org.springframework.stereotype.Component;

@Component
public final class WalletMutationStrategyResolver {
    private static final String TYPE_REQUIRED_MESSAGE = "Transaction type must not be null";

    private final WalletMutationStrategy creditStrategy = new CreditMutationStrategy();
    private final WalletMutationStrategy debitStrategy = new DebitMutationStrategy();

    public WalletMutationStrategy resolve(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException(TYPE_REQUIRED_MESSAGE);
        }

        return switch (type) {
            case TOPUP, REFUND -> creditStrategy;
            case PAYMENT, WITHDRAW -> debitStrategy;
        };
    }
}
