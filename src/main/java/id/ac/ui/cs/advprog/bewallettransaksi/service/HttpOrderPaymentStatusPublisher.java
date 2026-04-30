package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.util.Map;

import org.springframework.web.client.RestTemplate;

public class HttpOrderPaymentStatusPublisher implements OrderPaymentStatusPublisher {

    private static final String SETTLED_PATH = "/internal/orders/payment/settled";
    private static final String FAILED_PATH = "/internal/orders/payment/failed";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpOrderPaymentStatusPublisher(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public void publishPaymentSettled(String orderId) {
        postStatus(SETTLED_PATH, orderId);
    }

    @Override
    public void publishPaymentFailed(String orderId) {
        postStatus(FAILED_PATH, orderId);
    }

    private void postStatus(String path, String orderId) {
        restTemplate.postForEntity(baseUrl + path, Map.of("orderId", normalizeOrderId(orderId)), Void.class);
    }

    private String normalizeOrderId(String rawOrderId) {
        if (rawOrderId == null || rawOrderId.isBlank()) {
            throw new IllegalArgumentException("Order ID must not be blank");
        }
        return rawOrderId.trim();
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Order service base URL must not be blank");
        }
        return rawBaseUrl.endsWith("/") ? rawBaseUrl.substring(0, rawBaseUrl.length() - 1) : rawBaseUrl;
    }
}
