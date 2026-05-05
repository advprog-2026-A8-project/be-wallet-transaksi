package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;

public interface PaymentCallbackProcessor {
    void process(PaymentCallbackRequest payload);
}
