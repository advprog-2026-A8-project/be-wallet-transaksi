package id.ac.ui.cs.advprog.bewallettransaksi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class MidtransPaymentGatewayClientTest {

    private static final String SANDBOX_SERVER_KEY = "SB-Mid-server-test-key";
    private static final String SANDBOX_API_BASE_URL = "https://app.sandbox.midtrans.com";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void createTopUpInstruction_ShouldCallSnapApiAndMapResponse() {
        UUID userId = UUID.randomUUID();
        String orderId = "TOPUP-123";
        MidtransPaymentGatewayClient client =
                new MidtransPaymentGatewayClient(restTemplate, SANDBOX_SERVER_KEY, SANDBOX_API_BASE_URL);

        mockServer.expect(requestTo(SANDBOX_API_BASE_URL + "/snap/v1/transactions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic U0ItTWlkLXNlcnZlci10ZXN0LWtleTo="))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "transaction_details": {
                            "order_id": "TOPUP-123",
                            "gross_amount": 10000
                          },
                          "credit_card": {
                            "secure": true
                          },
                          "customer_details": {
                            "first_name": "%s"
                          }
                        }
                        """.formatted(userId)))
                .andRespond(withSuccess("""
                        {
                          "token": "snap-token-123",
                          "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/test"
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, String> instruction =
                client.createTopUpInstruction(userId, new BigDecimal("10000"), orderId);

        assertEquals("snap-token-123", instruction.get("paymentToken"));
        assertEquals("https://app.sandbox.midtrans.com/snap/v2/vtweb/test", instruction.get("redirectUrl"));
        assertEquals(orderId, instruction.get("orderId"));
        mockServer.verify();
    }

    @Test
    void createTopUpInstruction_WithDecimalAmount_ShouldThrowIllegalArgumentException() {
        MidtransPaymentGatewayClient client =
                new MidtransPaymentGatewayClient(restTemplate, SANDBOX_SERVER_KEY, SANDBOX_API_BASE_URL);
        UUID userId = UUID.randomUUID();
        BigDecimal invalidAmount = new BigDecimal("10000.50");
        String orderId = "TOPUP-123";

        assertThrows(
                IllegalArgumentException.class,
                () -> client.createTopUpInstruction(userId, invalidAmount, orderId)
        );
    }

    @Test
    void createTopUpInstruction_WhenResponseMissingToken_ShouldThrowIllegalStateException() {
        MidtransPaymentGatewayClient client =
                new MidtransPaymentGatewayClient(restTemplate, SANDBOX_SERVER_KEY, SANDBOX_API_BASE_URL);

        mockServer.expect(requestTo(SANDBOX_API_BASE_URL + "/snap/v1/transactions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/test"
                        }
                        """, MediaType.APPLICATION_JSON));

        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10000");
        String orderId = "TOPUP-123";

        assertThrows(
                IllegalStateException.class,
                () -> client.createTopUpInstruction(userId, amount, orderId)
        );
    }

    @Test
    void createTopUpInstruction_WithCustomTransactionPath_ShouldCallConfiguredPath() {
        UUID userId = UUID.randomUUID();
        String orderId = "TOPUP-123";
        String customPath = "/custom/snap/transactions";
        MidtransPaymentGatewayClient client =
                new MidtransPaymentGatewayClient(restTemplate, SANDBOX_SERVER_KEY, SANDBOX_API_BASE_URL, customPath);

        mockServer.expect(requestTo(SANDBOX_API_BASE_URL + customPath))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "token": "snap-token-123",
                          "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/test"
                        }
                        """, MediaType.APPLICATION_JSON));

        Map<String, String> instruction =
                client.createTopUpInstruction(userId, new BigDecimal("10000"), orderId);

        assertEquals("snap-token-123", instruction.get("paymentToken"));
        mockServer.verify();
    }
}
