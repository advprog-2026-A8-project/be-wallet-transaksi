package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class NoOpPaymentCallbackProcessor implements PaymentCallbackProcessor {

    @Override
    public void process(Map<String, Object> payload) {
        // Placeholder implementation. Real callback business handling is added incrementally via TDD cycles.
    }
}
