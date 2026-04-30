package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.service.strategy.WalletMutationStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceHistoryTest {

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
    void getTransactionHistory_Success() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        Transaction latest = new Transaction();
        latest.setTransactionId(UUID.randomUUID());
        latest.setWalletId(walletId);
        latest.setAmount(BigDecimal.valueOf(50.00));
        latest.setType(TransactionType.PAYMENT);
        latest.setStatus(TransactionStatus.SUCCESS);
        latest.setDescription("Latest payment");
        latest.setCreatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0));

        Transaction older = new Transaction();
        older.setTransactionId(UUID.randomUUID());
        older.setWalletId(walletId);
        older.setAmount(BigDecimal.valueOf(20.00));
        older.setType(TransactionType.REFUND);
        older.setStatus(TransactionStatus.SUCCESS);
        older.setDescription("Older refund");
        older.setCreatedAt(LocalDateTime.of(2026, 3, 22, 9, 0, 0));

        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId)).thenReturn(List.of(latest, older));

        List<TransactionResponse> responses = walletService.getTransactionHistory(userId);

        assertEquals(2, responses.size());
        assertEquals("Latest payment", responses.get(0).getDescription());
        assertEquals("Older refund", responses.get(1).getDescription());
        verify(walletRepository).findByUserId(userId);
        verify(transactionRepository).findByWalletIdOrderByCreatedAtDesc(walletId);
    }

    @Test
    void getTransactionHistoryByStatus_Success() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        Transaction failedWithdraw = new Transaction();
        failedWithdraw.setTransactionId(UUID.randomUUID());
        failedWithdraw.setWalletId(walletId);
        failedWithdraw.setAmount(BigDecimal.valueOf(40.00));
        failedWithdraw.setType(TransactionType.WITHDRAW);
        failedWithdraw.setStatus(TransactionStatus.FAILED);
        failedWithdraw.setDescription("Withdraw failed");
        failedWithdraw.setCreatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0));

        when(transactionRepository.findByWalletIdAndStatusOrderByCreatedAtDesc(walletId, TransactionStatus.FAILED))
                .thenReturn(List.of(failedWithdraw));

        List<TransactionResponse> responses = walletService.getTransactionHistoryByStatus(userId, TransactionStatus.FAILED);

        assertEquals(1, responses.size());
        assertEquals(TransactionStatus.FAILED, responses.get(0).getStatus());
        assertEquals("Withdraw failed", responses.get(0).getDescription());
        verify(walletRepository).findByUserId(userId);
        verify(transactionRepository).findByWalletIdAndStatusOrderByCreatedAtDesc(walletId, TransactionStatus.FAILED);
    }

    @Test
    void getTransactionHistory_WalletNotFound() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getTransactionHistory(userId));

        verify(walletRepository).findByUserId(userId);
        verify(transactionRepository, never()).findByWalletIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getTransactionHistoryByStatus_NullStatus_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> walletService.getTransactionHistoryByStatus(userId, null));
        verify(walletRepository, never()).findByUserId(any());
        verify(transactionRepository, never()).findByWalletIdAndStatusOrderByCreatedAtDesc(any(), any());
    }
}
