package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import org.springframework.stereotype.Component;

@Component
public class NoOpPaymentCallbackProcessor implements PaymentCallbackProcessor {

    @Override
    public void process(PaymentCallbackRequest payload) {
        // Placeholder implementation. Real callback business handling is added incrementally via TDD cycles.
    }
}
