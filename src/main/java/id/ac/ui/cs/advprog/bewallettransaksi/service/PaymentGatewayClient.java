package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface PaymentGatewayClient {
    Map<String, String> createTopUpInstruction(UUID userId, BigDecimal amount, String orderId);
}

