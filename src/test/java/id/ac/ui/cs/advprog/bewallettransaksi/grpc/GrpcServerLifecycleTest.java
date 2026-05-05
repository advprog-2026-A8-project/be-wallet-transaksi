package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrpcServerLifecycleTest {

    @Test
    void start_Disabled_ShouldNotRunServer() {
        GrpcServerProperties properties = new GrpcServerProperties();
        properties.setEnabled(false);
        properties.setPort(9090);
        properties.setInternalToken("internal-token");

        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(
                properties,
                new WalletContractGrpcService(new StubOrderWalletContractService())
        );

        lifecycle.start();

        assertFalse(lifecycle.isRunning());
    }

    @Test
    void start_EnabledWithoutInternalToken_ShouldThrow() {
        GrpcServerProperties properties = new GrpcServerProperties();
        properties.setEnabled(true);
        properties.setPort(9090);
        properties.setInternalToken("  ");

        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(
                properties,
                new WalletContractGrpcService(new StubOrderWalletContractService())
        );

        assertThrows(IllegalStateException.class, lifecycle::start);
    }
}
