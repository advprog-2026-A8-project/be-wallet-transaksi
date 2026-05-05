package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GrpcInternalAuthInterceptorTest {

    @Test
    void interceptCall_InvalidToken_ShouldCloseUnauthenticated() {
        GrpcInternalAuthInterceptor interceptor = new GrpcInternalAuthInterceptor("expected-token");
        TestServerCall call = new TestServerCall();
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("x-service-token", Metadata.ASCII_STRING_MARSHALLER), "wrong-token");

        interceptor.interceptCall(call, metadata, new NoopServerCallHandler());

        assertNotNull(call.closedStatus);
        assertEquals(Status.Code.UNAUTHENTICATED, call.closedStatus.getCode());
    }

    @Test
    void interceptCall_ValidToken_ShouldPassThrough() {
        GrpcInternalAuthInterceptor interceptor = new GrpcInternalAuthInterceptor("expected-token");
        TestServerCall call = new TestServerCall();
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("x-service-token", Metadata.ASCII_STRING_MARSHALLER), "expected-token");

        interceptor.interceptCall(call, metadata, new NoopServerCallHandler());

        assertNull(call.closedStatus);
    }

    private static final class NoopServerCallHandler implements ServerCallHandler<String, String> {
        @Override
        public ServerCall.Listener<String> startCall(ServerCall<String, String> call, Metadata headers) {
            return new ServerCall.Listener<>() {
            };
        }
    }

    private static final class TestServerCall extends ServerCall<String, String> {
        private Status closedStatus;

        @Override
        public void request(int numMessages) {
            // no-op for test
        }

        @Override
        public void sendHeaders(Metadata headers) {
            // no-op for test
        }

        @Override
        public void sendMessage(String message) {
            // no-op for test
        }

        @Override
        public void close(Status status, Metadata trailers) {
            closedStatus = status;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<String, String> getMethodDescriptor() {
            return MethodDescriptor.<String, String>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName("wallet.Contract/Test")
                    .setRequestMarshaller(stringMarshaller())
                    .setResponseMarshaller(stringMarshaller())
                    .build();
        }

        private MethodDescriptor.Marshaller<String> stringMarshaller() {
            return new MethodDescriptor.Marshaller<>() {
                @Override
                public InputStream stream(String value) {
                    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public String parse(InputStream stream) {
                    try {
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (Exception ex) {
                        return "";
                    }
                }
            };
        }
    }
}
