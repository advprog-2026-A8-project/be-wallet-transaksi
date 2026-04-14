package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class WalletMutationStrategyResolver {

    public WalletMutationStrategy resolve(TransactionType type) {
        return switch (type) {
            case TOPUP, REFUND -> new CreditMutationStrategy();
            case PAYMENT, WITHDRAW -> new DebitMutationStrategy();
        };
    }
}
