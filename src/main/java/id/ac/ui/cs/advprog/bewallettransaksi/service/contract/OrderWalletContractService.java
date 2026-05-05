package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

public interface OrderWalletContractService {
    CheckBalanceResult checkBalance(CheckBalanceRequest request);
    WalletMutationResult deductBalance(DeductBalanceRequest request);
    WalletMutationResult refundBalance(RefundBalanceRequest request);
}

