package id.ac.ui.cs.advprog.bewallettransaksi.grpc;

import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceResult;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.DeductBalanceRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.OrderWalletContractService;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.RefundBalanceRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.WalletMutationResult;

import java.math.BigDecimal;

class StubOrderWalletContractService implements OrderWalletContractService {

    @Override
    public CheckBalanceResult checkBalance(CheckBalanceRequest request) {
        return new CheckBalanceResult(true, BigDecimal.ZERO);
    }

    @Override
    public WalletMutationResult deductBalance(DeductBalanceRequest request) {
        return new WalletMutationResult(true, BigDecimal.ZERO, null);
    }

    @Override
    public WalletMutationResult refundBalance(RefundBalanceRequest request) {
        return new WalletMutationResult(true, BigDecimal.ZERO, null);
    }
}
