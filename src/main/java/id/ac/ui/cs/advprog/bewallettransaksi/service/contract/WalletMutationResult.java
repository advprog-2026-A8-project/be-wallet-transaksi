package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import java.math.BigDecimal;

public record WalletMutationResult(boolean success, BigDecimal updatedBalance, String errorCode) {
}

