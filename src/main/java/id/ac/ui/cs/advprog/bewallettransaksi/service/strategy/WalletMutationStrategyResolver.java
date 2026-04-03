package id.ac.ui.cs.advprog.bewallettransaksi.service.strategy;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;

public class WalletMutationStrategyResolver {

    public WalletMutationStrategy resolve(TransactionType type) {
        return switch (type) {
            case TOPUP, REFUND -> new CreditMutationStrategy();
            case PAYMENT, WITHDRAW -> new DebitMutationStrategy();
        };
    }
}

