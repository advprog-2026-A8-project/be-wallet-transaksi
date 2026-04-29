package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.Map;

public interface PaymentCallbackProcessor {
    void process(Map<String, Object> payload);
}
