package id.ac.ui.cs.advprog.bewallettransaksi.service;

public interface OrderPaymentStatusPublisher {
    void publishPaymentSettled(String orderId);
    void publishPaymentFailed(String orderId);
}
