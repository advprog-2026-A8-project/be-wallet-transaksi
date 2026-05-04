package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private final GrpcServerProperties properties;
    private final WalletContractGrpcService walletContractGrpcService;

    private volatile boolean running;
    private Server server;

    public GrpcServerLifecycle(
            GrpcServerProperties properties,
            WalletContractGrpcService walletContractGrpcService
    ) {
        this.properties = properties;
        this.walletContractGrpcService = walletContractGrpcService;
    }

    @Override
    public void start() {
        if (!properties.isEnabled()) {
            return;
        }
        validateInternalToken();
        try {
            server = NettyServerBuilder
                    .forPort(properties.getPort())
                    .addService(ServerInterceptors.intercept(
                            walletContractGrpcService,
                            new GrpcInternalAuthInterceptor(properties.getInternalToken())
                    ))
                    .build()
                    .start();
            running = true;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start gRPC server", ex);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdownNow();
            server = null;
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    private void validateInternalToken() {
        String token = properties.getInternalToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("grpc.server.internal-token must not be blank when grpc.server.enabled=true");
        }
    }
}
