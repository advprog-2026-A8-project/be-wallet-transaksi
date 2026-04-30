package id.ac.ui.cs.advprog.bewallettransaksi.service;

public class NoOpOrderPaymentStatusPublisher implements OrderPaymentStatusPublisher {
    @Override
    public void publishPaymentSettled(String orderId) {
        // No-op until Order module integration is wired.
    }

    @Override
    public void publishPaymentFailed(String orderId) {
        // No-op until Order module integration is wired.
    }
}
