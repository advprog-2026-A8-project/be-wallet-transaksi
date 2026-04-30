package id.ac.ui.cs.advprog.bewallettransaksi.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private HttpOrderPaymentStatusPublisher publisher;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        publisher = new HttpOrderPaymentStatusPublisher(restTemplate, "http://order-service");
    }

    @Test
    void publishPaymentSettled_ShouldPostToOrderService() {
        mockServer.expect(requestTo("http://order-service/internal/orders/payment/settled"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        publisher.publishPaymentSettled("ORDER-123");

        mockServer.verify();
    }

    @Test
    void publishPaymentFailed_ShouldPostToOrderService() {
        mockServer.expect(requestTo("http://order-service/internal/orders/payment/failed"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

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
}
