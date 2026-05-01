package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.util.Map;

import org.springframework.web.client.RestTemplate;

public class HttpOrderPaymentStatusPublisher implements OrderPaymentStatusPublisher {

    private static final String DEFAULT_SETTLED_PATH = "/internal/orders/payment/settled";
    private static final String DEFAULT_FAILED_PATH = "/internal/orders/payment/failed";
    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String FAILED_STATUS = "FAILED";
    private static final int MAX_ORDER_ID_LENGTH = 128;
    private static final String ORDER_ID_BLANK_MESSAGE = "Order ID must not be blank";
    private static final String ORDER_ID_TOO_LONG_MESSAGE = "Order ID exceeds maximum length";
    private static final String BASE_URL_BLANK_MESSAGE = "Order service base URL must not be blank";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String settledPath;
    private final String failedPath;

    public HttpOrderPaymentStatusPublisher(RestTemplate restTemplate, String baseUrl) {
        this(restTemplate, baseUrl, DEFAULT_SETTLED_PATH, DEFAULT_FAILED_PATH);
    }

    HttpOrderPaymentStatusPublisher(
            RestTemplate restTemplate,
            String baseUrl,
            String settledPath,
            String failedPath
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.settledPath = normalizePath(settledPath, DEFAULT_SETTLED_PATH);
        this.failedPath = normalizePath(failedPath, DEFAULT_FAILED_PATH);
    }

    @Override
    public void publishPaymentSettled(String orderId) {
        postStatus(settledPath, orderId, SUCCESS_STATUS);
    }

    @Override
    public void publishPaymentFailed(String orderId) {
        postStatus(failedPath, orderId, FAILED_STATUS);
    }

    private void postStatus(String path, String orderId, String status) {
        restTemplate.postForEntity(
                baseUrl + path,
                Map.of(
                        "orderId", normalizeOrderId(orderId),
                        "status", status
                ),
                Void.class
        );
    }

    private String normalizeOrderId(String rawOrderId) {
        if (rawOrderId == null || rawOrderId.isBlank()) {
            throw new IllegalArgumentException(ORDER_ID_BLANK_MESSAGE);
        }
        String normalizedOrderId = rawOrderId.trim();
        if (normalizedOrderId.length() > MAX_ORDER_ID_LENGTH) {
            throw new IllegalArgumentException(ORDER_ID_TOO_LONG_MESSAGE);
        }
        return normalizedOrderId;
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            throw new IllegalArgumentException(BASE_URL_BLANK_MESSAGE);
        }
        String trimmedBaseUrl = rawBaseUrl.trim();
        return trimmedBaseUrl.endsWith("/")
                ? trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1)
                : trimmedBaseUrl;
    }

    private String normalizePath(String rawPath, String defaultPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return defaultPath;
        }
        String normalizedPath = rawPath.trim();
        return normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
    }
}
