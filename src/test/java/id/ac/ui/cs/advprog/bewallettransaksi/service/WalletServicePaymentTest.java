package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.InvalidAmountException;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.service.strategy.WalletMutationStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServicePaymentTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Spy
    private WalletMutationStrategyResolver strategyResolver = new WalletMutationStrategyResolver();

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
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        Transaction pendingTransaction = transactionCaptor.getAllValues().get(0);
        Transaction successTransaction = transactionCaptor.getAllValues().get(1);

        assertEquals(walletId, pendingTransaction.getWalletId());
        assertEquals(BigDecimal.valueOf(60.00), pendingTransaction.getAmount());
        assertEquals(TransactionType.PAYMENT, pendingTransaction.getType());
        assertEquals(TransactionStatus.PENDING, pendingTransaction.getStatus());
        assertEquals("Order payment", pendingTransaction.getDescription());

        assertEquals(walletId, successTransaction.getWalletId());
        assertEquals(BigDecimal.valueOf(60.00), successTransaction.getAmount());
        assertEquals(TransactionType.PAYMENT, successTransaction.getType());
        assertEquals(TransactionStatus.SUCCESS, successTransaction.getStatus());
        assertEquals("Order payment", successTransaction.getDescription());
    }

    @Test
    void pay_ShouldPersistPendingThenSuccessTransaction() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.pay(userId, BigDecimal.valueOf(60.00), "Order payment");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        Transaction firstSave = transactionCaptor.getAllValues().get(0);
        Transaction secondSave = transactionCaptor.getAllValues().get(1);

        assertEquals(TransactionStatus.PENDING, firstSave.getStatus());
        assertEquals(TransactionStatus.SUCCESS, secondSave.getStatus());
        assertEquals(firstSave.getWalletId(), secondSave.getWalletId());
        assertEquals(firstSave.getAmount(), secondSave.getAmount());
    }

    @Test
    void pay_InsufficientBalance() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        BigDecimal amount = BigDecimal.valueOf(150.00);

        assertThrows(IllegalStateException.class,
                () -> walletService.pay(userId, amount, "Order payment"));

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository).save(any(Transaction.class));
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
        BigDecimal amount = BigDecimal.ZERO;
        assertThrows(InvalidAmountException.class,
                () -> walletService.pay(userId, amount, "Order payment"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_NegativeAmount() {
        BigDecimal amount = BigDecimal.valueOf(-1.00);
        assertThrows(InvalidAmountException.class,
                () -> walletService.pay(userId, amount, "Order payment"));

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
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void pay_AmountJustAboveBalance() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BigDecimal amount = BigDecimal.valueOf(100.01);

        assertThrows(IllegalStateException.class,
                () -> walletService.pay(userId, amount, "Order payment"));

        verify(walletRepository).findByUserIdForUpdate(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void pay_BlankDescription_ShouldThrowIllegalArgumentException() {
        BigDecimal amount = BigDecimal.valueOf(10.00);
        assertThrows(IllegalArgumentException.class,
                () -> walletService.pay(userId, amount, "   "));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_NullDescription_ShouldThrowIllegalArgumentException() {
        BigDecimal amount = BigDecimal.valueOf(10.00);
        assertThrows(IllegalArgumentException.class,
                () -> walletService.pay(userId, amount, null));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void pay_NullUserId_ShouldThrowIllegalArgumentException() {
        BigDecimal amount = BigDecimal.valueOf(10.00);
        assertThrows(IllegalArgumentException.class,
                () -> walletService.pay(null, amount, "Order payment"));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
