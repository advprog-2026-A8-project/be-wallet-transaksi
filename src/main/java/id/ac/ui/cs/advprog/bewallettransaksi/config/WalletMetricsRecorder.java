package id.ac.ui.cs.advprog.bewallettransaksi.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class WalletMetricsRecorder {
    private final Counter topupInitiatedTotal;
    private final Counter topupSuccessTotal;
    private final Counter topupFailedTotal;
    private final Counter paymentDeductSuccessTotal;
    private final Counter paymentDeductFailedTotal;
    private final Counter refundSuccessTotal;
    private final Counter refundFailedTotal;
    private final Counter callbackSettlementTotal;
    private final Counter callbackFailureTotal;
    private final Counter callbackIdempotentTotal;
    private final Counter callbackOutOfOrderNoopTotal;

    public WalletMetricsRecorder(MeterRegistry meterRegistry) {
        this.topupInitiatedTotal = Counter.builder("wallet.topup.initiated.total")
                .description("Total number of topup initiate requests")
                .register(meterRegistry);
        this.topupSuccessTotal = Counter.builder("wallet.topup.success.total")
                .description("Total number of successful topup mutations")
                .register(meterRegistry);
        this.topupFailedTotal = Counter.builder("wallet.topup.failed.total")
                .description("Total number of failed topup operations")
                .register(meterRegistry);
        this.paymentDeductSuccessTotal = Counter.builder("wallet.payment.deduct.success.total")
                .description("Total number of successful payment deductions")
                .register(meterRegistry);
        this.paymentDeductFailedTotal = Counter.builder("wallet.payment.deduct.failed.total")
                .description("Total number of failed payment deductions")
                .register(meterRegistry);
        this.refundSuccessTotal = Counter.builder("wallet.refund.success.total")
                .description("Total number of successful refunds")
                .register(meterRegistry);
        this.refundFailedTotal = Counter.builder("wallet.refund.failed.total")
                .description("Total number of failed refunds")
                .register(meterRegistry);
        this.callbackSettlementTotal = Counter.builder("wallet.callback.settlement.total")
                .description("Total number of callback settlements")
                .register(meterRegistry);
        this.callbackFailureTotal = Counter.builder("wallet.callback.failure.total")
                .description("Total number of callback failures")
                .register(meterRegistry);
        this.callbackIdempotentTotal = Counter.builder("wallet.callback.idempotent.total")
                .description("Total number of idempotent callback transitions")
                .register(meterRegistry);
        this.callbackOutOfOrderNoopTotal = Counter.builder("wallet.callback.out_of_order.noop.total")
                .description("Total number of out-of-order callback no-op transitions")
                .register(meterRegistry);
    }

    public void incrementTopupInitiated() {
        topupInitiatedTotal.increment();
    }

    public void incrementTopupSuccess() {
        topupSuccessTotal.increment();
    }

    public void incrementTopupFailed() {
        topupFailedTotal.increment();
    }

    public void incrementPaymentDeductSuccess() {
        paymentDeductSuccessTotal.increment();
    }

    public void incrementPaymentDeductFailed() {
        paymentDeductFailedTotal.increment();
    }

    public void incrementRefundSuccess() {
        refundSuccessTotal.increment();
    }

    public void incrementRefundFailed() {
        refundFailedTotal.increment();
    }

    public void incrementCallbackSettlement() {
        callbackSettlementTotal.increment();
    }

    public void incrementCallbackFailure() {
        callbackFailureTotal.increment();
    }

    public void incrementCallbackIdempotent() {
        callbackIdempotentTotal.increment();
    }

    public void incrementCallbackOutOfOrderNoop() {
        callbackOutOfOrderNoopTotal.increment();
    }
}
