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
class WalletServiceRefundTest {

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
    void refund_Success() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.refund(userId, BigDecimal.valueOf(25.00), "Order refund");

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(125.00), response.getBalance());

        verify(walletRepository).findByUserId(userId);
        verify(walletRepository).save(wallet);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(walletId, savedTransaction.getWalletId());
        assertEquals(BigDecimal.valueOf(25.00), savedTransaction.getAmount());
        assertEquals(TransactionType.REFUND, savedTransaction.getType());
        assertEquals(TransactionStatus.SUCCESS, savedTransaction.getStatus());
        assertEquals("Order refund", savedTransaction.getDescription());
    }

    @Test
    void refund_WalletNotFound() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.refund(userId, BigDecimal.valueOf(25.00), "Order refund"));

        verify(walletRepository).findByUserId(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void refund_NullAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.refund(userId, null, "Order refund"));

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void refund_ZeroAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.refund(userId, BigDecimal.ZERO, "Order refund"));

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void refund_NegativeAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.refund(userId, BigDecimal.valueOf(-1.00), "Order refund"));

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
