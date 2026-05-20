package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.util.Objects;

public record OrderPaymentStatusEvent(String orderId, OrderPaymentStatus status) {

    public OrderPaymentStatusEvent {
        Objects.requireNonNull(status, "Order payment status must not be null");
    }

    public static OrderPaymentStatusEvent settled(String orderId) {
        return new OrderPaymentStatusEvent(orderId, OrderPaymentStatus.SUCCESS);
    }

    public static OrderPaymentStatusEvent failed(String orderId) {
        return new OrderPaymentStatusEvent(orderId, OrderPaymentStatus.FAILED);
    }
}
