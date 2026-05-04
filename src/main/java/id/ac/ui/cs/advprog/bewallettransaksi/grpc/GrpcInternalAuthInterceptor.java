package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

public class GrpcInternalAuthInterceptor implements ServerInterceptor {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid internal service token";
    private static final Metadata.Key<String> SERVICE_TOKEN_HEADER =
            Metadata.Key.of("x-service-token", Metadata.ASCII_STRING_MARSHALLER);

    private final String expectedToken;

    public GrpcInternalAuthInterceptor(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String actualToken = headers.get(SERVICE_TOKEN_HEADER);
        if (!isAuthorized(actualToken)) {
            call.close(Status.UNAUTHENTICATED.withDescription(INVALID_TOKEN_MESSAGE), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
        return next.startCall(call, headers);
    }

    private boolean isAuthorized(String actualToken) {
        return expectedToken != null
                && !expectedToken.isBlank()
                && expectedToken.equals(actualToken);
    }
}
