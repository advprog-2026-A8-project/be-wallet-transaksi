package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NoOpPaymentCallbackProcessor implements PaymentCallbackProcessor {
    private static final Logger log = LoggerFactory.getLogger(NoOpPaymentCallbackProcessor.class);
    private static final String EVENT_KEY_DELIMITER = "|";

    private final WalletService walletService;
    private final Set<String> processedCallbackEvents = ConcurrentHashMap.newKeySet();
    private final Map<String, String> terminalStatusByOrderId = new ConcurrentHashMap<>();

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
        if (shouldIgnoreTerminalEvent(orderId, status)) {
            log.info("wallet.callback.processor.ignore_terminal orderId={} status={}", orderId, status);
            return;
        }
        String eventKey = buildEventKey(orderId, status);
        if (!processedCallbackEvents.add(eventKey)) {
            log.info("wallet.callback.processor.duplicate_event orderId={} status={}", orderId, status);
            return;
        }
        if (MidtransTransactionStatus.isSettlement(status)) {
            log.info("wallet.callback.processor.dispatch_settlement orderId={}", orderId);
            walletService.handlePaymentSettlement(orderId);
            return;
        }
        if (MidtransTransactionStatus.isFailure(status)) {
            log.info("wallet.callback.processor.dispatch_failure orderId={}", orderId);
            walletService.handlePaymentFailure(orderId);
        }
    }

    private boolean shouldIgnoreTerminalEvent(String orderId, String status) {
        if (!isTerminalStatus(status)) {
            return false;
        }
        String existingStatus = terminalStatusByOrderId.putIfAbsent(orderId, status);
        return existingStatus != null && !existingStatus.equals(status);
    }

    private boolean isTerminalStatus(String status) {
        return MidtransTransactionStatus.isSettlement(status) || MidtransTransactionStatus.isFailure(status);
    }

    private String buildEventKey(String orderId, String status) {
        return orderId + EVENT_KEY_DELIMITER + status;
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
