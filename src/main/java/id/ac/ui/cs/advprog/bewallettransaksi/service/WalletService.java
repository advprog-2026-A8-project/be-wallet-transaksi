package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.util.UUID;
import java.math.BigDecimal;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;

public interface WalletService {
    WalletResponse getWallet(UUID userId);
    WalletResponse createWallet(UUID userId);
    WalletResponse topUp(TopUpRequest request);
    WalletResponse pay(UUID userId, BigDecimal amount, String description);
}
