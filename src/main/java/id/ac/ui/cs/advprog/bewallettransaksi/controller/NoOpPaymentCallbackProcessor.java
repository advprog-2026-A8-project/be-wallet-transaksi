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
        Optional<String> normalizedStatus = normalizeNonBlank(payload.getTransactionStatus());
        if (normalizedStatus.isEmpty()) {
            return;
        }
        Optional<String> normalizedOrderId = normalizeNonBlank(payload.getOrderId());
        if (normalizedOrderId.isEmpty()) {
            return;
        }
        String orderId = normalizedOrderId.get();
        String status = normalizedStatus.get();
        if (MidtransTransactionStatus.isSettlement(status)) {
            walletService.handlePaymentSettlement(orderId);
            return;
        }
        if (MidtransTransactionStatus.isFailure(status)) {
            walletService.handlePaymentFailure(orderId);
        }
    }

    private Optional<String> normalizeNonBlank(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }
}
