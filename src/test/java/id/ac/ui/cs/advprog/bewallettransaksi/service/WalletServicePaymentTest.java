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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WalletServicePaymentTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OrderPaymentStatusPublisher orderPaymentStatusPublisher;

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
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction successTransaction = transactionCaptor.getValue();

        assertEquals(walletId, successTransaction.getWalletId());
        assertEquals(BigDecimal.valueOf(60.00), successTransaction.getAmount());
        assertEquals(TransactionType.PAYMENT, successTransaction.getType());
        assertEquals(TransactionStatus.SUCCESS, successTransaction.getStatus());
        assertEquals("Order payment", successTransaction.getDescription());
    }

    @Test
    void pay_ShouldPersistSuccessTransaction() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.pay(userId, BigDecimal.valueOf(60.00), "Order payment");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionStatus.SUCCESS, savedTransaction.getStatus());
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
        verify(transactionRepository).save(any(Transaction.class));
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

    @Test
    void handlePaymentSettlement_ShouldPersistSuccessForMatchingPendingPayment() {
        Transaction pendingPayment = new Transaction();
        pendingPayment.setTransactionId(UUID.randomUUID());
        pendingPayment.setWalletId(walletId);
        pendingPayment.setAmount(BigDecimal.valueOf(60.00));
        pendingPayment.setType(TransactionType.PAYMENT);
        pendingPayment.setStatus(TransactionStatus.PENDING);
        pendingPayment.setDescription("ORDER-1");

        when(transactionRepository.findAll()).thenReturn(List.of(pendingPayment));

        walletService.handlePaymentSettlement("ORDER-1");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.SUCCESS, transactionCaptor.getValue().getStatus());
        verify(orderPaymentStatusPublisher).publishPaymentSettled("ORDER-1");
    }

    @Test
    void handlePaymentFailure_ShouldPersistFailedForMatchingPendingPayment() {
        Transaction pendingPayment = new Transaction();
        pendingPayment.setTransactionId(UUID.randomUUID());
        pendingPayment.setWalletId(walletId);
        pendingPayment.setAmount(BigDecimal.valueOf(60.00));
        pendingPayment.setType(TransactionType.PAYMENT);
        pendingPayment.setStatus(TransactionStatus.PENDING);
        pendingPayment.setDescription("ORDER-2");

        when(transactionRepository.findAll()).thenReturn(List.of(pendingPayment));

        walletService.handlePaymentFailure("ORDER-2");

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.FAILED, transactionCaptor.getValue().getStatus());
        verify(orderPaymentStatusPublisher).publishPaymentFailed("ORDER-2");
    }

    @Test
    void handlePaymentFailure_OrderIdNotFound_ShouldThrowIllegalStateException() {
        when(transactionRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> walletService.handlePaymentFailure("ORDER-NOT-FOUND"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void handlePaymentFailure_AlreadyFailedTransaction_ShouldBeNoOp() {
        Transaction failedPayment = new Transaction();
        failedPayment.setTransactionId(UUID.randomUUID());
        failedPayment.setWalletId(walletId);
        failedPayment.setAmount(BigDecimal.valueOf(60.00));
        failedPayment.setType(TransactionType.PAYMENT);
        failedPayment.setStatus(TransactionStatus.FAILED);
        failedPayment.setDescription("ORDER-3");

        when(transactionRepository.findAll()).thenReturn(List.of(failedPayment));

        assertDoesNotThrow(() -> walletService.handlePaymentFailure("ORDER-3"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void handlePaymentSettlement_AlreadySuccessfulTransaction_ShouldBeNoOp() {
        Transaction successPayment = new Transaction();
        successPayment.setTransactionId(UUID.randomUUID());
        successPayment.setWalletId(walletId);
        successPayment.setAmount(BigDecimal.valueOf(60.00));
        successPayment.setType(TransactionType.PAYMENT);
        successPayment.setStatus(TransactionStatus.SUCCESS);
        successPayment.setDescription("ORDER-4");

        when(transactionRepository.findAll()).thenReturn(List.of(successPayment));

        assertDoesNotThrow(() -> walletService.handlePaymentSettlement("ORDER-4"));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(orderPaymentStatusPublisher);
    }

    @Test
    void handlePaymentSettlement_FromFailedStatus_ShouldThrowIllegalStateException() {
        Transaction failedPayment = new Transaction();
        failedPayment.setTransactionId(UUID.randomUUID());
        failedPayment.setWalletId(walletId);
        failedPayment.setAmount(BigDecimal.valueOf(60.00));
        failedPayment.setType(TransactionType.PAYMENT);
        failedPayment.setStatus(TransactionStatus.FAILED);
        failedPayment.setDescription("ORDER-5");

        when(transactionRepository.findAll()).thenReturn(List.of(failedPayment));

        assertThrows(IllegalStateException.class, () -> walletService.handlePaymentSettlement("ORDER-5"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void handlePaymentFailure_FromSuccessStatus_ShouldThrowIllegalStateException() {
        Transaction successPayment = new Transaction();
        successPayment.setTransactionId(UUID.randomUUID());
        successPayment.setWalletId(walletId);
        successPayment.setAmount(BigDecimal.valueOf(60.00));
        successPayment.setType(TransactionType.PAYMENT);
        successPayment.setStatus(TransactionStatus.SUCCESS);
        successPayment.setDescription("ORDER-6");

        when(transactionRepository.findAll()).thenReturn(List.of(successPayment));

        assertThrows(IllegalStateException.class, () -> walletService.handlePaymentFailure("ORDER-6"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
