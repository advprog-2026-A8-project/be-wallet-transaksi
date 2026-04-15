package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.InvalidAmountException;
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
class WalletServicePaymentTest {

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
    void pay_Success() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.pay(userId, BigDecimal.valueOf(60.00), "Order payment");

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(40.00), response.getBalance());

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository).save(wallet);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(walletId, savedTransaction.getWalletId());
        assertEquals(BigDecimal.valueOf(60.00), savedTransaction.getAmount());
        assertEquals(TransactionType.PAYMENT, savedTransaction.getType());
        assertEquals(TransactionStatus.SUCCESS, savedTransaction.getStatus());
        assertEquals("Order payment", savedTransaction.getDescription());
    }

    @Test
    void pay_InsufficientBalance() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalStateException.class,
                () -> walletService.pay(userId, BigDecimal.valueOf(150.00), "Order payment"));

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_NullAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.pay(userId, null, "Order payment"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_ZeroAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.pay(userId, BigDecimal.ZERO, "Order payment"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_NegativeAmount() {
        assertThrows(InvalidAmountException.class,
                () -> walletService.pay(userId, BigDecimal.valueOf(-1.00), "Order payment"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_ExactBalance() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.pay(userId, BigDecimal.valueOf(100.00), "Order payment");

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getBalance());
        verify(walletRepository).save(wallet);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void pay_AmountJustAboveBalance() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        assertThrows(IllegalStateException.class,
                () -> walletService.pay(userId, BigDecimal.valueOf(100.01), "Order payment"));

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_BlankDescription_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> walletService.pay(userId, BigDecimal.valueOf(10.00), "   "));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
