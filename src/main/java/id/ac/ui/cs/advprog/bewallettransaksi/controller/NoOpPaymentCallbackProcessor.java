package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.springframework.stereotype.Component;

@Component
public class NoOpPaymentCallbackProcessor implements PaymentCallbackProcessor {

    private final WalletService walletService;

    public NoOpPaymentCallbackProcessor(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public void process(PaymentCallbackRequest payload) {
        if (payload == null || payload.getTransactionStatus() == null) {
            return;
        }
        if (MidtransTransactionStatus.isSettlement(payload.getTransactionStatus())) {
            walletService.handlePaymentSettlement(payload.getOrderId());
        }
    }
}
