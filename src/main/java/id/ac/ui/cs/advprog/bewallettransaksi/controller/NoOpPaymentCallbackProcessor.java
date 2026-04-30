package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoOpPaymentCallbackProcessor implements PaymentCallbackProcessor {

    private final WalletService walletService;

    public NoOpPaymentCallbackProcessor(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public void process(PaymentCallbackRequest payload) {
        if (payload == null) {
            return;
        }
        Optional<String> normalizedStatus = normalizeStatus(payload.getTransactionStatus());
        if (normalizedStatus.isEmpty()) {
            return;
        }
        String status = normalizedStatus.get();
        if (MidtransTransactionStatus.isSettlement(status)) {
            walletService.handlePaymentSettlement(payload.getOrderId());
            return;
        }
        if (MidtransTransactionStatus.isFailure(status)) {
            walletService.handlePaymentFailure(payload.getOrderId());
        }
    }

    private Optional<String> normalizeStatus(String status) {
        if (status == null) {
            return Optional.empty();
        }
        String normalized = status.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }
}
