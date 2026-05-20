package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcInternalAuthInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(GrpcInternalAuthInterceptor.class);

    private static final String INVALID_TOKEN_MESSAGE = "Invalid internal service token";
    private static final Metadata.Key<String> SERVICE_TOKEN_HEADER =
            Metadata.Key.of("x-service-token", Metadata.ASCII_STRING_MARSHALLER);

    private final String expectedToken;

    public GrpcInternalAuthInterceptor(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public <R, S> ServerCall.Listener<R> interceptCall(
            ServerCall<R, S> call,
            Metadata headers,
            ServerCallHandler<R, S> next
    ) {
        String actualToken = headers.get(SERVICE_TOKEN_HEADER);
        if (!isAuthorized(actualToken)) {
            log.warn("wallet.grpc.auth.failed method={}", call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription(INVALID_TOKEN_MESSAGE), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
        log.info("wallet.grpc.auth.success method={}", call.getMethodDescriptor().getFullMethodName());
        return next.startCall(call, headers);
    }

    private boolean isAuthorized(String actualToken) {
        return expectedToken != null
                && !expectedToken.isBlank()
                && expectedToken.equals(actualToken);
    }
}
