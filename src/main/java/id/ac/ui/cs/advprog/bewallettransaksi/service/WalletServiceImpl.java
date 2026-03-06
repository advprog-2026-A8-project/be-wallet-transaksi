package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.InvalidAmountException;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public WalletResponse getWallet(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
        return toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse createWallet(UUID userId) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        Wallet saved = walletRepository.save(wallet);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public WalletResponse topUp(TopUpRequest request) {
        validateAmount(request.getAmount());

        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new WalletNotFoundException(request.getUserId()));

        Transaction transaction = createTransaction(
                wallet.getWalletId(),
                request.getAmount(),
                TransactionType.TOPUP,
                "Top-up saldo"
        );

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        transaction.setStatus(TransactionStatus.SUCCESS);

        walletRepository.save(wallet);
        transactionRepository.save(transaction);

        return toResponse(wallet);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
    }

    private Transaction createTransaction(UUID walletId, BigDecimal amount,
                                          TransactionType type, String description) {
        Transaction transaction = new Transaction();
        transaction.setWalletId(walletId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription(description);
        return transaction;
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .build();
    }
}
