package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.springframework.stereotype.Service;

@Service
public class DefaultOrderWalletContractService implements OrderWalletContractService {

    private static final String ERROR_WALLET_NOT_FOUND = "WALLET_NOT_FOUND";
    private static final String ERROR_INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    private static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";

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
            return new WalletMutationResult(true, response.getBalance(), null);
        } catch (WalletNotFoundException ex) {
            return new WalletMutationResult(false, null, ERROR_WALLET_NOT_FOUND);
        } catch (IllegalStateException ex) {
            return new WalletMutationResult(false, null, ERROR_INSUFFICIENT_BALANCE);
        } catch (IllegalArgumentException ex) {
            return new WalletMutationResult(false, null, ERROR_INVALID_REQUEST);
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
            return new WalletMutationResult(true, response.getBalance(), null);
        } catch (WalletNotFoundException ex) {
            return new WalletMutationResult(false, null, ERROR_WALLET_NOT_FOUND);
        } catch (IllegalStateException ex) {
            return new WalletMutationResult(false, null, ERROR_INSUFFICIENT_BALANCE);
        } catch (IllegalArgumentException ex) {
            return new WalletMutationResult(false, null, ERROR_INVALID_REQUEST);
        }
    }
}

