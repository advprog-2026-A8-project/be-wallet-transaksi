package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class HttpOrderPaymentStatusPublisher implements OrderPaymentStatusPublisher {
    private static final Logger log = LoggerFactory.getLogger(HttpOrderPaymentStatusPublisher.class);

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_ORDER_ID_LENGTH = 128;
    private static final String ORDER_ID_BLANK_MESSAGE = "Order ID must not be blank";
    private static final String ORDER_ID_TOO_LONG_MESSAGE = "Order ID exceeds maximum length";
    private static final String BASE_URL_BLANK_MESSAGE = "Order service base URL must not be blank";
    private static final String INTERNAL_AUTHORIZATION_INVALID_MESSAGE =
            "Order service internal authorization must be blank or use 'Bearer <token>' format";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String settledPath;
    private final String failedPath;
    private final String internalAuthorization;

    HttpOrderPaymentStatusPublisher(
            RestTemplate restTemplate,
            String baseUrl,
            String settledPath,
            String failedPath,
            String internalAuthorization
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.settledPath = normalizePath(settledPath, "settledPath");
        this.failedPath = normalizePath(failedPath, "failedPath");
        this.internalAuthorization = normalizeInternalAuthorization(internalAuthorization);
    }

    @Override
    public void publish(OrderPaymentStatusEvent event) {
        String path = resolveTargetPath(event.status());
        postStatus(path, event);
    }

    private String resolveTargetPath(OrderPaymentStatus status) {
        return switch (status) {
            case SUCCESS -> settledPath;
            case FAILED -> failedPath;
        };
    }

    private void postStatus(String path, OrderPaymentStatusEvent event) {
        String normalizedOrderId = normalizeOrderId(event.orderId());
        String targetUrl = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalAuthorization != null) {
            headers.set(HEADER_AUTHORIZATION, internalAuthorization);
        }
        try {
            restTemplate.postForEntity(
                    targetUrl,
                    new HttpEntity<>(
                            Map.of(
                                    "orderId", normalizedOrderId,
                                    "status", event.status().name()
                            ),
                            headers
                    ),
                    Void.class
            );
            log.info(
                    "wallet.order.publisher.success orderId={} status={} target={}",
                    normalizedOrderId,
                    event.status(),
                    path
            );
        } catch (RuntimeException ex) {
            log.error(
                    "wallet.order.publisher.failed orderId={} status={} target={} error={}",
                    normalizedOrderId,
                    event.status(),
                    path,
                    ex.toString()
            );
            throw new IllegalStateException(
                    "Failed to publish order payment status: orderId="
                            + normalizedOrderId
                            + ", status="
                            + event.status()
                            + ", target="
                            + path,
                    ex
            );
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

    private String normalizeInternalAuthorization(String rawInternalAuthorization) {
        if (rawInternalAuthorization == null || rawInternalAuthorization.isBlank()) {
            return null;
        }
        String normalized = rawInternalAuthorization.trim();
        if (!normalized.startsWith(BEARER_PREFIX) || normalized.length() <= BEARER_PREFIX.length()) {
            throw new IllegalArgumentException(INTERNAL_AUTHORIZATION_INVALID_MESSAGE);
        }
        return normalized;
    }
}
