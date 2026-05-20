package id.ac.ui.cs.advprog.bewallettransaksi.service;

public class NoOpOrderPaymentStatusPublisher implements OrderPaymentStatusPublisher {
    @Override
    public void publish(OrderPaymentStatusEvent event) {
        // No-op until Order module integration is wired.
    }
}
