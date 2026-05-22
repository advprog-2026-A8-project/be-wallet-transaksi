package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrpcServerLifecycleExtendedTest {

    @Test
    void lifecycleMetadata_ShouldMatchExpectedDefaults() {
        GrpcServerProperties properties = new GrpcServerProperties();
        properties.setEnabled(false);
        properties.setPort(9090);
        properties.setInternalToken("internal-token");

        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(
                properties,
                new WalletContractGrpcService(new StubOrderWalletContractService())
        );

        assertTrue(lifecycle.isAutoStartup());
        assertEquals(Integer.MAX_VALUE, lifecycle.getPhase());
        assertFalse(lifecycle.isRunning());
    }

    @Test
    void stopRunnable_ShouldInvokeCallbackEvenWhenServerNeverStarted() {
        GrpcServerProperties properties = new GrpcServerProperties();
        properties.setEnabled(false);
        properties.setPort(9090);
        properties.setInternalToken("internal-token");

        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(
                properties,
                new WalletContractGrpcService(new StubOrderWalletContractService())
        );

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        lifecycle.stop(() -> callbackInvoked.set(true));

        assertTrue(callbackInvoked.get());
        assertFalse(lifecycle.isRunning());
    }

    @Test
    void startAndStop_WhenEnabledWithToken_ShouldToggleRunningState() {
        GrpcServerProperties properties = new GrpcServerProperties();
        properties.setEnabled(true);
        properties.setPort(0);
        properties.setInternalToken("internal-token");

        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(
                properties,
                new WalletContractGrpcService(new StubOrderWalletContractService())
        );

        lifecycle.start();
        assertTrue(lifecycle.isRunning());

        lifecycle.stop();
        assertFalse(lifecycle.isRunning());
    }
}

