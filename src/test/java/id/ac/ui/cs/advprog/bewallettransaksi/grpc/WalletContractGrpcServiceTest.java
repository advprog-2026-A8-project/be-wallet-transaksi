package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceResult;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.OrderWalletContractService;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.WalletMutationResult;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletContractGrpcServiceTest {

    @Mock
    private OrderWalletContractService contractService;

    private WalletContractGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new WalletContractGrpcService(contractService);
    }

    @Test
    void checkBalance_ShouldReturnContractResult() {
        UUID userId = UUID.randomUUID();
        when(contractService.checkBalance(
                new id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceRequest(
                        userId,
                        new BigDecimal("100.00")
                )
        ))
                .thenReturn(new CheckBalanceResult(true, new BigDecimal("150.00")));

        TestStreamObserver<CheckBalanceResponse> observer = new TestStreamObserver<>();
        grpcService.checkBalance(
                CheckBalanceRequest.newBuilder()
                        .setUserId(userId.toString())
                        .setAmount("100.00")
                        .build(),
                observer
        );

        assertNull(observer.error);
        assertNotNull(observer.value);
        assertTrue(observer.completed);
        assertTrue(observer.value.getSufficient());
        assertEquals("150.00", observer.value.getCurrentBalance());
    }

    @Test
    void deductBalance_InvalidAmount_ShouldReturnInvalidArgumentStatus() {
        TestStreamObserver<WalletMutationResponse> observer = new TestStreamObserver<>();
        grpcService.deductBalance(
                DeductBalanceRequest.newBuilder()
                        .setUserId(UUID.randomUUID().toString())
                        .setOrderId("ORDER-1")
                        .setAmount("abc")
                        .setIdempotencyKey("idem-1")
                        .build(),
                observer
        );

        assertNotNull(observer.error);
        Status status = Status.fromThrowable(observer.error);
        assertEquals(Status.Code.INVALID_ARGUMENT, status.getCode());
    }

    @Test
    void refundBalance_ShouldMapMutationResult() {
        UUID userId = UUID.randomUUID();
        when(contractService.refundBalance(
                new id.ac.ui.cs.advprog.bewallettransaksi.service.contract.RefundBalanceRequest(
                        userId,
                        "ORDER-2",
                        new BigDecimal("20.00"),
                        "idem-2"
                )
        ))
                .thenReturn(new WalletMutationResult(false, null, "ORDER_NOT_FOUND"));

        TestStreamObserver<WalletMutationResponse> observer = new TestStreamObserver<>();
        grpcService.refundBalance(
                RefundBalanceRequest.newBuilder()
                        .setUserId(userId.toString())
                        .setOrderId("ORDER-2")
                        .setAmount("20.00")
                        .setIdempotencyKey("idem-2")
                        .build(),
                observer
        );

        assertNull(observer.error);
        assertNotNull(observer.value);
        assertTrue(observer.completed);
        assertEquals(false, observer.value.getSuccess());
        assertEquals("ORDER_NOT_FOUND", observer.value.getErrorCode());
        assertEquals(false, observer.value.getRetryable());
        assertEquals("", observer.value.getUpdatedBalance());
    }

    private static class TestStreamObserver<T> implements StreamObserver<T> {
        private T value;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }
}
