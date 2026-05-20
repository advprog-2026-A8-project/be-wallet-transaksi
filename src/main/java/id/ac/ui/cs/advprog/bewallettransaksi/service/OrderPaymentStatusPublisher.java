package id.ac.ui.cs.advprog.bewallettransaksi.service;

public interface OrderPaymentStatusPublisher {
    void publish(OrderPaymentStatusEvent event);

    default void publishPaymentSettled(String orderId) {
        publish(OrderPaymentStatusEvent.settled(orderId));
    }

    default void publishPaymentFailed(String orderId) {
        publish(OrderPaymentStatusEvent.failed(orderId));
    }
}
