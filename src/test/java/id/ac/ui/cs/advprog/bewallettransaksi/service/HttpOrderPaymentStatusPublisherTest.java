package id.ac.ui.cs.advprog.bewallettransaksi.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HttpOrderPaymentStatusPublisherTest {

    private static final String BASE_URL = "http://order-service";
    private static final String SETTLED_PATH = "/internal/orders/payment/settled";
    private static final String FAILED_PATH = "/internal/orders/payment/failed";
    private static final String INTERNAL_AUTHORIZATION = "Bearer wallet-service-token";
    private static final int MAX_ORDER_ID_LENGTH = 128;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private HttpOrderPaymentStatusPublisher publisher;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        publisher = new HttpOrderPaymentStatusPublisher(restTemplate, BASE_URL, SETTLED_PATH, FAILED_PATH, "");
    }

    @Test
    void publishPaymentSettled_ShouldPostToOrderService() {
        expectPostSuccess(SETTLED_PATH, "{\"orderId\":\"ORDER-123\",\"status\":\"SUCCESS\"}");

        publisher.publishPaymentSettled("ORDER-123");

        mockServer.verify();
    }

    @Test
    void publishPaymentFailed_ShouldPostToOrderService() {
        expectPostSuccess(FAILED_PATH, "{\"orderId\":\"ORDER-123\",\"status\":\"FAILED\"}");

        publisher.publishPaymentFailed("ORDER-123");

        mockServer.verify();
    }

    @Test
    void publishPaymentSettled_BlankOrderId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publishPaymentSettled("   "));
    }

    @Test
    void publishPaymentFailed_NullOrderId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publishPaymentFailed(null));
    }

    @Test
    void constructor_BaseUrlWithOuterWhitespace_ShouldBeTrimmed() {
        HttpOrderPaymentStatusPublisher publisherWithSpacedBaseUrl =
                new HttpOrderPaymentStatusPublisher(
                        restTemplate,
                        "  " + BASE_URL + "  ",
                        SETTLED_PATH,
                        FAILED_PATH,
                        ""
                );
        expectPostSuccess(SETTLED_PATH, "{\"orderId\":\"ORDER-999\",\"status\":\"SUCCESS\"}");

        publisherWithSpacedBaseUrl.publishPaymentSettled("ORDER-999");

        mockServer.verify();
    }

    @Test
    void publishPaymentSettled_OrderIdTooLong_ShouldThrowIllegalArgumentException() {
        String tooLongOrderId = "O".repeat(MAX_ORDER_ID_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> publisher.publishPaymentSettled(tooLongOrderId));
    }

    @Test
    void publishPaymentSettled_WithInternalAuthorization_ShouldSendAuthorizationHeader() {
        HttpOrderPaymentStatusPublisher authorizedPublisher =
                new HttpOrderPaymentStatusPublisher(
                        restTemplate,
                        BASE_URL,
                        SETTLED_PATH,
                        FAILED_PATH,
                        INTERNAL_AUTHORIZATION
                );
        mockServer.expect(requestTo(BASE_URL + SETTLED_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", INTERNAL_AUTHORIZATION))
                .andExpect(content().json("{\"orderId\":\"ORDER-777\",\"status\":\"SUCCESS\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        authorizedPublisher.publishPaymentSettled("ORDER-777");

        mockServer.verify();
    }

    @Test
    void constructor_InvalidInternalAuthorization_ShouldThrowIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpOrderPaymentStatusPublisher(
                        restTemplate,
                        BASE_URL,
                        SETTLED_PATH,
                        FAILED_PATH,
                        "invalid-token"
                )
        );
    }

    @Test
    void publishPaymentSettled_WhenOrderServiceFails_ShouldWrapWithContext() {
        mockServer.expect(requestTo(BASE_URL + SETTLED_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publisher.publishPaymentSettled("ORDER-500")
        );

        assertTrue(exception.getMessage().contains("ORDER-500"));
        assertTrue(exception.getMessage().contains("SUCCESS"));
    }

    private void expectPostSuccess(String path, String expectedJsonBody) {
        mockServer.expect(requestTo(BASE_URL + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedJsonBody))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    }
}
