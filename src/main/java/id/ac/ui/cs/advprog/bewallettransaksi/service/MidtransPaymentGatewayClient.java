package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class MidtransPaymentGatewayClient implements PaymentGatewayClient {

    private static final String PAYMENT_TOKEN_KEY = "paymentToken";
    private static final String REDIRECT_URL_KEY = "redirectUrl";
    private static final String ORDER_ID_KEY = "orderId";
    private static final String MIDTRANS_TOKEN_KEY = "token";
    private static final String MIDTRANS_REDIRECT_URL_KEY = "redirect_url";
    private static final String DEFAULT_SNAP_TRANSACTION_PATH = "/snap/v1/transactions";
    private static final String DEFAULT_SANDBOX_API_BASE = "https://app.sandbox.midtrans.com";
    private static final String DEFAULT_PRODUCTION_API_BASE = "https://app.midtrans.com";
    private static final String SERVER_KEY_REQUIRED_MESSAGE = "Midtrans server key must not be blank";
    private static final String WHOLE_NUMBER_AMOUNT_REQUIRED_MESSAGE =
            "Midtrans top-up amount must be a whole number";

    private final RestTemplate restTemplate;
    private final String serverKey;
    private final String configuredApiBaseUrl;
    private final String snapTransactionPath;

    @Autowired
    public MidtransPaymentGatewayClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${midtrans.server-key:}") String serverKey,
            @Value("${midtrans.snap.api-base-url:}") String configuredApiBaseUrl,
            @Value("${midtrans.snap.transaction-path:" + DEFAULT_SNAP_TRANSACTION_PATH + "}") String snapTransactionPath
    ) {
        this(
                restTemplateBuilder
                        .connectTimeout(Duration.ofSeconds(5))
                        .readTimeout(Duration.ofSeconds(10))
                        .build(),
                serverKey,
                configuredApiBaseUrl,
                snapTransactionPath
        );
    }

    MidtransPaymentGatewayClient(
            RestTemplate restTemplate,
            String serverKey,
            String configuredApiBaseUrl
    ) {
        this(restTemplate, serverKey, configuredApiBaseUrl, DEFAULT_SNAP_TRANSACTION_PATH);
    }

    MidtransPaymentGatewayClient(
            RestTemplate restTemplate,
            String serverKey,
            String configuredApiBaseUrl,
            String snapTransactionPath
    ) {
        this.restTemplate = restTemplate;
        this.serverKey = serverKey == null ? "" : serverKey.trim();
        this.configuredApiBaseUrl = configuredApiBaseUrl == null ? "" : configuredApiBaseUrl.trim();
        this.snapTransactionPath = normalizeSnapTransactionPath(snapTransactionPath);
    }

    @Override
    public Map<String, String> createTopUpInstruction(UUID userId, BigDecimal amount, String orderId) {
        validateServerKey();
        String apiBaseUrl = resolveApiBaseUrl();
        long grossAmount = toWholeNumberAmount(amount);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(
                buildRequestBody(userId, grossAmount, orderId),
                buildHeaders()
        );

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + snapTransactionPath,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() { }
            );
            return mapSnapResponse(response.getBody(), orderId);
        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Failed to create Midtrans Snap transaction for orderId=" + orderId,
                    ex
            );
        }
    }

    private void validateServerKey() {
        if (!StringUtils.hasText(serverKey)) {
            throw new IllegalStateException(SERVER_KEY_REQUIRED_MESSAGE);
        }
    }

    private String resolveApiBaseUrl() {
        if (StringUtils.hasText(configuredApiBaseUrl)) {
            return trimTrailingSlash(configuredApiBaseUrl);
        }
        if (serverKey.startsWith("SB-")) {
            return DEFAULT_SANDBOX_API_BASE;
        }
        return DEFAULT_PRODUCTION_API_BASE;
    }

    private String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String normalizeSnapTransactionPath(String configuredPath) {
        if (!StringUtils.hasText(configuredPath)) {
            return DEFAULT_SNAP_TRANSACTION_PATH;
        }
        String normalizedPath = configuredPath.trim();
        return normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
    }

    private long toWholeNumberAmount(BigDecimal amount) {
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(WHOLE_NUMBER_AMOUNT_REQUIRED_MESSAGE, ex);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(serverKey, "");
        return headers;
    }

    private Map<String, Object> buildRequestBody(UUID userId, long grossAmount, String orderId) {
        return Map.of(
                "transaction_details", Map.of(
                        "order_id", orderId,
                        "gross_amount", grossAmount
                ),
                "credit_card", Map.of(
                        "secure", true
                ),
                "customer_details", Map.of(
                        "first_name", userId.toString()
                )
        );
    }

    private Map<String, String> mapSnapResponse(Map<?, ?> responseBody, String orderId) {
        if (responseBody == null) {
            throw new IllegalStateException("Midtrans Snap response body is empty for orderId=" + orderId);
        }
        Object token = responseBody.get(MIDTRANS_TOKEN_KEY);
        Object redirectUrl = responseBody.get(MIDTRANS_REDIRECT_URL_KEY);
        if (!(token instanceof String tokenValue) || tokenValue.isBlank()
                || !(redirectUrl instanceof String redirectUrlValue) || redirectUrlValue.isBlank()) {
            throw new IllegalStateException(
                    "Midtrans Snap response missing token/redirect_url for orderId=" + orderId
            );
        }
        return Map.of(
                PAYMENT_TOKEN_KEY, tokenValue,
                REDIRECT_URL_KEY, redirectUrlValue,
                ORDER_ID_KEY, orderId
        );
    }
}

