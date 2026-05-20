package id.ac.ui.cs.advprog.bewallettransaksi.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WalletMetricsRecorderTest {

    private SimpleMeterRegistry meterRegistry;
    private WalletMetricsRecorder recorder;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        recorder = new WalletMetricsRecorder(meterRegistry);
    }

    @Test
    void incrementCounters_ShouldUpdateAllBusinessCounters() {
        recorder.incrementTopupInitiated();
        recorder.incrementTopupSuccess();
        recorder.incrementTopupFailed();
        recorder.incrementPaymentDeductSuccess();
        recorder.incrementPaymentDeductFailed();
        recorder.incrementRefundSuccess();
        recorder.incrementRefundFailed();
        recorder.incrementCallbackSettlement();
        recorder.incrementCallbackFailure();
        recorder.incrementCallbackIdempotent();
        recorder.incrementCallbackOutOfOrderNoop();
        recorder.incrementIdempotencyConflict();
        recorder.incrementPublisherFailure();
        recorder.incrementAuthLookupFailure();
        recorder.incrementGrpcRequest();
        recorder.incrementGrpcError();

        assertEquals(1.0, meterRegistry.get("wallet.topup.initiated.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.topup.success.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.topup.failed.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.payment.deduct.success.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.payment.deduct.failed.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.refund.success.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.refund.failed.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.callback.settlement.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.callback.failure.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.callback.idempotent.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.callback.out_of_order.noop.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.idempotency.conflict.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.order.publisher.failure.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.auth.lookup.failure.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.grpc.request.total").counter().count());
        assertEquals(1.0, meterRegistry.get("wallet.grpc.error.total").counter().count());
    }

    @Test
    void recordDurations_ShouldUpdateTimersForRunnableAndNanosVariants() {
        recorder.recordPayDuration(() -> {
        });
        recorder.recordPayDurationNanos(10);
        recorder.recordRefundDuration(() -> {
        });
        recorder.recordRefundDurationNanos(20);
        recorder.recordTopupInitiateDuration(() -> "ok");
        recorder.recordTopupInitiateDurationNanos(30);
        recorder.recordCallbackDuration(() -> {
        });
        recorder.recordCallbackDurationNanos(40);
        recorder.recordPublisherDuration(() -> {
        });
        recorder.recordPublisherDurationNanos(50);

        assertTimerCount("wallet.pay.duration", 2);
        assertTimerCount("wallet.refund.duration", 2);
        assertTimerCount("wallet.topup.initiate.duration", 2);
        assertTimerCount("wallet.callback.duration", 2);
        assertTimerCount("wallet.order.publisher.duration", 2);
    }

    @Test
    void recordDurationNanos_WithNegativeValues_ShouldClampToZeroWithoutFailing() {
        recorder.recordPayDurationNanos(-1);
        recorder.recordRefundDurationNanos(-1);
        recorder.recordTopupInitiateDurationNanos(-1);
        recorder.recordCallbackDurationNanos(-1);
        recorder.recordPublisherDurationNanos(-1);

        assertEquals(1L, meterRegistry.get("wallet.pay.duration").timer().count());
        assertEquals(1L, meterRegistry.get("wallet.refund.duration").timer().count());
        assertEquals(1L, meterRegistry.get("wallet.topup.initiate.duration").timer().count());
        assertEquals(1L, meterRegistry.get("wallet.callback.duration").timer().count());
        assertEquals(1L, meterRegistry.get("wallet.order.publisher.duration").timer().count());
    }

    @Test
    void gauges_ShouldTrackActiveLocksAndCacheSize() {
        Gauge activeLocksGauge = meterRegistry.get("wallet.callback.locks.active").gauge();
        Gauge cacheSizeGauge = meterRegistry.get("wallet.topup.initiate.cache.size").gauge();

        assertNotNull(activeLocksGauge);
        assertNotNull(cacheSizeGauge);
        assertEquals(0.0, activeLocksGauge.value());
        assertEquals(0.0, cacheSizeGauge.value());

        recorder.incrementActiveCallbackLocks();
        recorder.incrementActiveCallbackLocks();
        recorder.decrementActiveCallbackLocks();
        recorder.updateTopupInitiateCacheSize(7);

        assertEquals(1.0, activeLocksGauge.value());
        assertEquals(7.0, cacheSizeGauge.value());
    }

    private void assertTimerCount(String meterName, long expectedCount) {
        Timer timer = meterRegistry.get(meterName).timer();
        assertEquals(expectedCount, timer.count());
        assertNotNull(timer.totalTime(TimeUnitForTest.NANOSECONDS.toMicrometerUnit()));
    }

    private enum TimeUnitForTest {
        NANOSECONDS;

        java.util.concurrent.TimeUnit toMicrometerUnit() {
            return java.util.concurrent.TimeUnit.NANOSECONDS;
        }
    }
}
