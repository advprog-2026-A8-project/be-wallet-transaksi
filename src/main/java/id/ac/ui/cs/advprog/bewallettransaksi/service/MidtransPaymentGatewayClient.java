package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MidtransPaymentGatewayClient implements PaymentGatewayClient {

    private static final String PAYMENT_TOKEN_KEY = "paymentToken";
    private static final String REDIRECT_URL_KEY = "redirectUrl";
    private static final String ORDER_ID_KEY = "orderId";
    private static final String DEFAULT_TOKEN_PREFIX = "midtrans-snap-";
    private static final String DEFAULT_REDIRECT_BASE = "https://snap.midtrans.com/checkout/";

    private final String tokenPrefix;
    private final String redirectBaseUrl;

    public MidtransPaymentGatewayClient(
            @Value("${midtrans.snap.token-prefix:" + DEFAULT_TOKEN_PREFIX + "}") String tokenPrefix,
            @Value("${midtrans.snap.redirect-base-url:" + DEFAULT_REDIRECT_BASE + "}") String redirectBaseUrl
    ) {
        this.tokenPrefix = tokenPrefix;
        this.redirectBaseUrl = redirectBaseUrl;
    }

    @Override
    public Map<String, String> createTopUpInstruction(UUID userId, BigDecimal amount, String orderId) {
        return Map.of(
                PAYMENT_TOKEN_KEY, tokenPrefix + UUID.randomUUID(),
                REDIRECT_URL_KEY, redirectBaseUrl + orderId,
                ORDER_ID_KEY, orderId
        );
    }
}

