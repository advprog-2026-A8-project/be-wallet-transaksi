package id.ac.ui.cs.advprog.bewallettransaksi.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OrderPaymentStatusPublisherConfigTest {

    private final OrderPaymentStatusPublisherConfig config = new OrderPaymentStatusPublisherConfig();

    @Test
    void httpOrderPaymentStatusPublisher_ShouldReturnHttpPublisher_WhenTimeoutNonPositive() {
        OrderPaymentStatusPublisher publisher = config.httpOrderPaymentStatusPublisher(
                new RestTemplateBuilder(),
                "http://order-service",
                0L,
                "/internal/orders/payment/settled",
                "/internal/orders/payment/failed",
                ""
        );

        assertInstanceOf(HttpOrderPaymentStatusPublisher.class, publisher);
    }

    @Test
    void noopOrderPaymentStatusPublisher_ShouldReturnNoopPublisher() {
        OrderPaymentStatusPublisher publisher = config.noopOrderPaymentStatusPublisher();

        assertInstanceOf(NoOpOrderPaymentStatusPublisher.class, publisher);
    }
}

