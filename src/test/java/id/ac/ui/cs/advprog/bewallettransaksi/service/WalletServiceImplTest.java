package id.ac.ui.cs.advprog.bewallettransaksi.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

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
    void getWallet_Success() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWallet(userId);

        assertNotNull(response);
        assertEquals(walletId, response.getWalletId());
        assertEquals(userId, response.getUserId());
        assertEquals(BigDecimal.valueOf(100.00), response.getBalance());
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void getWallet_NotFound() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWallet(userId));
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void createWallet_Success() {
        Wallet newWallet = new Wallet();
        newWallet.setWalletId(walletId);
        newWallet.setUserId(userId);
        newWallet.setBalance(BigDecimal.ZERO);

        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        WalletResponse response = walletService.createWallet(userId);

        assertNotNull(response);
        assertEquals(walletId, response.getWalletId());
        assertEquals(userId, response.getUserId());
        assertEquals(BigDecimal.ZERO, response.getBalance());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();
        assertEquals(userId, savedWallet.getUserId());
        assertEquals(BigDecimal.ZERO, savedWallet.getBalance());
    }

    @Test
    void topUp_Success() {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(50.00));

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.topUp(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(150.00), response.getBalance());

        verify(walletRepository).findByUserId(userId);
        verify(walletRepository).save(wallet);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(walletId, savedTransaction.getWalletId());
        assertEquals(BigDecimal.valueOf(50.00), savedTransaction.getAmount());
        assertEquals(TransactionType.TOPUP, savedTransaction.getType());
        assertEquals(TransactionStatus.SUCCESS, savedTransaction.getStatus());
        assertEquals("Top-up saldo", savedTransaction.getDescription());
    }

    @Test
    void topUp_WalletNotFound() {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(50.00));

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.topUp(request));
        verify(walletRepository).findByUserId(userId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void topUp_NullAmount() {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(null);

        assertThrows(InvalidAmountException.class, () -> walletService.topUp(request));
        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void topUp_ZeroAmount() {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.ZERO);

        assertThrows(InvalidAmountException.class, () -> walletService.topUp(request));
        verify(walletRepository, never()).findByUserId(any());
    }

    @Test
    void topUp_NegativeAmount() {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(-10.00));

        assertThrows(InvalidAmountException.class, () -> walletService.topUp(request));
        verify(walletRepository, never()).findByUserId(any());
    }
}
