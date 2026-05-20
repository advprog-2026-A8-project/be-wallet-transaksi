package id.ac.ui.cs.advprog.bewallettransaksi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class OrderPaymentStatusPublisherConfig {

    @Bean
    @ConditionalOnProperty(
            name = "order.service.publisher.enabled",
            havingValue = "true"
    )
    OrderPaymentStatusPublisher httpOrderPaymentStatusPublisher(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${order.service.base-url}") String baseUrl,
            @Value("${order.service.timeout-ms:1000}") long timeoutMs,
            @Value("${order.service.payment-settled-path:/internal/orders/payment/settled}") String settledPath,
            @Value("${order.service.payment-failed-path:/internal/orders/payment/failed}") String failedPath,
            @Value("${order.service.internal-authorization:}") String internalAuthorization
    ) {
        long safeTimeoutMs = timeoutMs > 0 ? timeoutMs : 1000;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) safeTimeoutMs);
        requestFactory.setReadTimeout((int) safeTimeoutMs);
        return new HttpOrderPaymentStatusPublisher(
                restTemplateBuilder
                        .requestFactory(() -> requestFactory)
                        .build(),
                baseUrl,
                settledPath,
                failedPath,
                internalAuthorization
        );
    }

    @Bean
    @ConditionalOnMissingBean(OrderPaymentStatusPublisher.class)
    OrderPaymentStatusPublisher noopOrderPaymentStatusPublisher() {
        return new NoOpOrderPaymentStatusPublisher();
    }
}
