package id.ac.ui.cs.advprog.bewallettransaksi.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

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
    private final Counter idempotencyConflictTotal;
    private final Counter publisherFailureTotal;
    private final Counter authLookupFailureTotal;
    private final Counter grpcRequestTotal;
    private final Counter grpcErrorTotal;
    private final Timer payDuration;
    private final Timer refundDuration;
    private final Timer topupInitiateDuration;
    private final Timer callbackDuration;
    private final Timer publisherDuration;
    private final AtomicInteger activeCallbackLocks = new AtomicInteger(0);
    private final AtomicInteger topupInitiateCacheSize = new AtomicInteger(0);

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
        this.idempotencyConflictTotal = Counter.builder("wallet.idempotency.conflict.total")
                .description("Total number of idempotency key conflicts")
                .register(meterRegistry);
        this.publisherFailureTotal = Counter.builder("wallet.order.publisher.failure.total")
                .description("Total number of order publisher failures")
                .register(meterRegistry);
        this.authLookupFailureTotal = Counter.builder("wallet.auth.lookup.failure.total")
                .description("Total number of auth username lookup failures")
                .register(meterRegistry);
        this.grpcRequestTotal = Counter.builder("wallet.grpc.request.total")
                .description("Total number of gRPC requests")
                .register(meterRegistry);
        this.grpcErrorTotal = Counter.builder("wallet.grpc.error.total")
                .description("Total number of gRPC errors")
                .register(meterRegistry);
        this.payDuration = Timer.builder("wallet.pay.duration")
                .description("Duration of wallet pay operation")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.refundDuration = Timer.builder("wallet.refund.duration")
                .description("Duration of wallet refund operation")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.topupInitiateDuration = Timer.builder("wallet.topup.initiate.duration")
                .description("Duration of wallet topup initiate operation")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.callbackDuration = Timer.builder("wallet.callback.duration")
                .description("Duration of wallet callback transition operation")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.publisherDuration = Timer.builder("wallet.order.publisher.duration")
                .description("Duration of order publisher operation")
                .publishPercentileHistogram()
                .register(meterRegistry);
        meterRegistry.gauge("wallet.callback.locks.active", activeCallbackLocks);
        meterRegistry.gauge("wallet.topup.initiate.cache.size", topupInitiateCacheSize);
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

    public void incrementIdempotencyConflict() {
        idempotencyConflictTotal.increment();
    }

    public void incrementPublisherFailure() {
        publisherFailureTotal.increment();
    }

    public void incrementAuthLookupFailure() {
        authLookupFailureTotal.increment();
    }

    public void incrementGrpcRequest() {
        grpcRequestTotal.increment();
    }

    public void incrementGrpcError() {
        grpcErrorTotal.increment();
    }

    public void recordPayDuration(Runnable action) {
        payDuration.record(action);
    }

    public void recordPayDurationNanos(long nanos) {
        payDuration.record(java.time.Duration.ofNanos(Math.max(0L, nanos)));
    }

    public void recordRefundDuration(Runnable action) {
        refundDuration.record(action);
    }

    public void recordRefundDurationNanos(long nanos) {
        refundDuration.record(java.time.Duration.ofNanos(Math.max(0L, nanos)));
    }

    public <T> T recordTopupInitiateDuration(java.util.function.Supplier<T> action) {
        return topupInitiateDuration.record(action);
    }

    public void recordTopupInitiateDurationNanos(long nanos) {
        topupInitiateDuration.record(java.time.Duration.ofNanos(Math.max(0L, nanos)));
    }

    public void recordCallbackDuration(Runnable action) {
        callbackDuration.record(action);
    }

    public void recordCallbackDurationNanos(long nanos) {
        callbackDuration.record(java.time.Duration.ofNanos(Math.max(0L, nanos)));
    }

    public void recordPublisherDuration(Runnable action) {
        publisherDuration.record(action);
    }

    public void recordPublisherDurationNanos(long nanos) {
        publisherDuration.record(java.time.Duration.ofNanos(Math.max(0L, nanos)));
    }

    public void incrementActiveCallbackLocks() {
        activeCallbackLocks.incrementAndGet();
    }

    public void decrementActiveCallbackLocks() {
        activeCallbackLocks.decrementAndGet();
    }

    public void updateTopupInitiateCacheSize(int cacheSize) {
        topupInitiateCacheSize.set(cacheSize);
    }
}
