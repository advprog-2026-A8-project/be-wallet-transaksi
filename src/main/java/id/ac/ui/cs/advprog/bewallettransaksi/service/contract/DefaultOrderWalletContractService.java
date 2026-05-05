package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.springframework.stereotype.Service;

@Service
public class DefaultOrderWalletContractService implements OrderWalletContractService {

    private static final String INSUFFICIENT_BALANCE_MESSAGE = "Insufficient balance";
    private static final String SUCCESSFUL_PAYMENT_NOT_FOUND_PREFIX =
            "Successful payment transaction not found";

    private final WalletService walletService;

    public DefaultOrderWalletContractService(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public CheckBalanceResult checkBalance(CheckBalanceRequest request) {
        WalletResponse wallet = walletService.getWallet(request.userId());
        boolean sufficient = wallet.getBalance().compareTo(request.amount()) >= 0;
        return new CheckBalanceResult(sufficient, wallet.getBalance());
    }

    @Override
    public WalletMutationResult deductBalance(DeductBalanceRequest request) {
        try {
            WalletResponse response = walletService.deductBalanceForOrder(
                    request.userId(),
                    request.orderId(),
                    request.amount(),
                    request.idempotencyKey()
            );
            return WalletMutationResult.success(response.getBalance());
        } catch (WalletNotFoundException ex) {
            return WalletMutationResult.failure(WalletContractErrorCode.WALLET_NOT_FOUND);
        } catch (IllegalStateException ex) {
            return mapIllegalState(ex);
        } catch (IllegalArgumentException ex) {
            return WalletMutationResult.failure(WalletContractErrorCode.INVALID_REQUEST);
        } catch (RuntimeException ex) {
            return WalletMutationResult.failure(WalletContractErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public WalletMutationResult refundBalance(RefundBalanceRequest request) {
        try {
            WalletResponse response = walletService.refundBalanceForOrder(
                    request.userId(),
                    request.orderId(),
                    request.amount(),
                    request.idempotencyKey()
            );
            return WalletMutationResult.success(response.getBalance());
        } catch (WalletNotFoundException ex) {
            return WalletMutationResult.failure(WalletContractErrorCode.WALLET_NOT_FOUND);
        } catch (IllegalStateException ex) {
            return mapIllegalState(ex);
        } catch (IllegalArgumentException ex) {
            return WalletMutationResult.failure(WalletContractErrorCode.INVALID_REQUEST);
        } catch (RuntimeException ex) {
            return WalletMutationResult.failure(WalletContractErrorCode.INTERNAL_ERROR);
        }
    }

    private WalletMutationResult mapIllegalState(IllegalStateException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return WalletMutationResult.failure(WalletContractErrorCode.INVALID_REQUEST);
        }
        if (message.contains(SUCCESSFUL_PAYMENT_NOT_FOUND_PREFIX)) {
            return WalletMutationResult.failure(WalletContractErrorCode.PAYMENT_NOT_FOUND);
        }
        if (message.contains(INSUFFICIENT_BALANCE_MESSAGE)) {
            return WalletMutationResult.failure(WalletContractErrorCode.INSUFFICIENT_BALANCE);
        }
        return WalletMutationResult.failure(WalletContractErrorCode.INVALID_REQUEST);
    }
}
