package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceResult;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.OrderWalletContractService;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.WalletMutationResult;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletContractGrpcService extends WalletContractServiceGrpc.WalletContractServiceImplBase {

    private final OrderWalletContractService contractService;

    public WalletContractGrpcService(OrderWalletContractService contractService) {
        this.contractService = contractService;
    }

    @Override
    public void checkBalance(
            id.ac.ui.cs.advprog.bewallettransaksi.grpc.CheckBalanceRequest request,
            StreamObserver<CheckBalanceResponse> responseObserver
    ) {
        try {
            CheckBalanceResult result = contractService.checkBalance(new id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceRequest(
                    parseUserId(request.getUserId()),
                    parseAmount(request.getAmount())
            ));
            responseObserver.onNext(CheckBalanceResponse.newBuilder()
                    .setSufficient(result.sufficient())
                    .setCurrentBalance(result.currentBalance().toPlainString())
                    .build());
            responseObserver.onCompleted();
        } catch (RuntimeException ex) {
            failInvalidArgument(responseObserver, ex);
        }
    }

    @Override
    public void deductBalance(
            id.ac.ui.cs.advprog.bewallettransaksi.grpc.DeductBalanceRequest request,
            StreamObserver<WalletMutationResponse> responseObserver
    ) {
        processMutation(() -> contractService.deductBalance(new id.ac.ui.cs.advprog.bewallettransaksi.service.contract.DeductBalanceRequest(
                parseUserId(request.getUserId()),
                request.getOrderId(),
                parseAmount(request.getAmount()),
                request.getIdempotencyKey()
        )), responseObserver);
    }

    @Override
    public void refundBalance(
            id.ac.ui.cs.advprog.bewallettransaksi.grpc.RefundBalanceRequest request,
            StreamObserver<WalletMutationResponse> responseObserver
    ) {
        processMutation(() -> contractService.refundBalance(new id.ac.ui.cs.advprog.bewallettransaksi.service.contract.RefundBalanceRequest(
                parseUserId(request.getUserId()),
                request.getOrderId(),
                parseAmount(request.getAmount()),
                request.getIdempotencyKey()
        )), responseObserver);
    }

    private void processMutation(
            MutationSupplier supplier,
            StreamObserver<WalletMutationResponse> responseObserver
    ) {
        try {
            WalletMutationResult result = supplier.get();
            responseObserver.onNext(toMutationResponse(result));
            responseObserver.onCompleted();
        } catch (RuntimeException ex) {
            failInvalidArgument(responseObserver, ex);
        }
    }

    private UUID parseUserId(String userId) {
        return UUID.fromString(userId);
    }

    private BigDecimal parseAmount(String amount) {
        return new BigDecimal(amount);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private WalletMutationResponse toMutationResponse(WalletMutationResult result) {
        WalletMutationResponse.Builder response = WalletMutationResponse.newBuilder()
                .setSuccess(result.success())
                .setErrorCode(defaultString(result.errorCode()));
        if (result.updatedBalance() != null) {
            response.setUpdatedBalance(result.updatedBalance().toPlainString());
        }
        return response.build();
    }

    private <T> void failInvalidArgument(StreamObserver<T> responseObserver, RuntimeException ex) {
        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
    }

    @FunctionalInterface
    private interface MutationSupplier {
        WalletMutationResult get();
    }
}
