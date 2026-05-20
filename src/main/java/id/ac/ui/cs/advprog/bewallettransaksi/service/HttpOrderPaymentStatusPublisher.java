package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

public class HttpOrderPaymentStatusPublisher implements OrderPaymentStatusPublisher {
    private static final Logger log = LoggerFactory.getLogger(HttpOrderPaymentStatusPublisher.class);

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

    HttpOrderPaymentStatusPublisher(
            RestTemplate restTemplate,
            String baseUrl,
            String settledPath,
            String failedPath
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.settledPath = normalizePath(settledPath, "settledPath");
        this.failedPath = normalizePath(failedPath, "failedPath");
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
        String normalizedOrderId = normalizeOrderId(orderId);
        String targetUrl = baseUrl + path;
        try {
            restTemplate.postForEntity(
                    targetUrl,
                    Map.of(
                            "orderId", normalizedOrderId,
                            "status", status
                    ),
                    Void.class
            );
            log.info("wallet.order.publisher.success orderId={} status={} target={}", normalizedOrderId, status, path);
        } catch (RuntimeException ex) {
            log.error("wallet.order.publisher.failed orderId={} status={} target={} error={}",
                    normalizedOrderId, status, path, ex.toString());
            throw ex;
        }
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

    private String normalizePath(String rawPath, String pathName) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException(pathName + " must not be blank");
        }
        String normalizedPath = rawPath.trim();
        return normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
    }
}
