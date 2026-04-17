package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;

public interface WalletService {
    WalletResponse getWallet(UUID userId);
    WalletResponse createWallet(UUID userId);
    WalletResponse topUp(TopUpRequest request);
    WalletResponse pay(UUID userId, BigDecimal amount, String description);
    WalletResponse refund(UUID userId, BigDecimal amount, String description);
    WalletResponse withdraw(UUID userId, BigDecimal amount, String description);
    List<TransactionResponse> getTransactionHistory(UUID userId);
    List<TransactionResponse> getTransactionHistoryByStatus(UUID userId, TransactionStatus status);
}
