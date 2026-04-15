package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.InvalidAmountException;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceWithdrawTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID userId;
    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.valueOf(100.00));
    }

    @Test
    void withdraw_Success() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.withdraw(userId, BigDecimal.valueOf(30.00), "BCA-123456");

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(70.00), response.getBalance());

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository).save(wallet);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(walletId, savedTransaction.getWalletId());
        assertEquals(BigDecimal.valueOf(30.00), savedTransaction.getAmount());
        assertEquals(TransactionType.WITHDRAW, savedTransaction.getType());
        assertEquals(TransactionStatus.SUCCESS, savedTransaction.getStatus());
        assertEquals("BCA-123456", savedTransaction.getDescription());
    }

    @Test
    void withdraw_WalletNotFound() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.withdraw(userId, BigDecimal.valueOf(30.00), "BCA-123456"));

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_InsufficientBalance() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalStateException.class,
                () -> walletService.withdraw(userId, BigDecimal.valueOf(120.00), "BCA-123456"));

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_InsufficientBalance_ShouldRecordFailedTransaction() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalStateException.class,
                () -> walletService.withdraw(userId, BigDecimal.valueOf(120.00), "BCA-123456"));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction failedTx = transactionCaptor.getValue();
        Assertions.assertEquals(walletId, failedTx.getWalletId());
        Assertions.assertEquals(BigDecimal.valueOf(120.00), failedTx.getAmount());
        Assertions.assertEquals(TransactionType.WITHDRAW, failedTx.getType());
        Assertions.assertEquals(TransactionStatus.FAILED, failedTx.getStatus());
        Assertions.assertEquals("BCA-123456", failedTx.getDescription());
    }

    @Test
    void withdraw_NullAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.withdraw(userId, null, "BCA-123456"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_ZeroAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.withdraw(userId, BigDecimal.ZERO, "BCA-123456"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_NegativeAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.withdraw(userId, BigDecimal.valueOf(-1.00), "BCA-123456"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
