package id.ac.ui.cs.advprog.bewallettransaksi.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
    private static final int MAX_ORDER_ID_LENGTH = 128;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private HttpOrderPaymentStatusPublisher publisher;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        publisher = new HttpOrderPaymentStatusPublisher(restTemplate, BASE_URL);
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
                new HttpOrderPaymentStatusPublisher(restTemplate, "  " + BASE_URL + "  ");
        expectPostSuccess(SETTLED_PATH, "{\"orderId\":\"ORDER-999\",\"status\":\"SUCCESS\"}");

        publisherWithSpacedBaseUrl.publishPaymentSettled("ORDER-999");

        mockServer.verify();
    }

    @Test
    void publishPaymentSettled_OrderIdTooLong_ShouldThrowIllegalArgumentException() {
        String tooLongOrderId = "O".repeat(MAX_ORDER_ID_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> publisher.publishPaymentSettled(tooLongOrderId));
    }

    private void expectPostSuccess(String path, String expectedJsonBody) {
        mockServer.expect(requestTo(BASE_URL + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedJsonBody))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    }
}
