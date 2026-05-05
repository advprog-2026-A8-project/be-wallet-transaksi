package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOrderWalletContractServiceTest {

    @Mock
    private WalletService walletService;

    private DefaultOrderWalletContractService contractService;

    @BeforeEach
    void setUp() {
        contractService = new DefaultOrderWalletContractService(walletService);
    }

    @Test
    void deductBalance_WalletNotFound_ShouldReturnWalletNotFoundError() {
        UUID userId = UUID.randomUUID();
        when(walletService.deductBalanceForOrder(userId, "ORDER-1", BigDecimal.TEN, "idem-1"))
                .thenThrow(new WalletNotFoundException(userId));

        WalletMutationResult result = contractService.deductBalance(
                new DeductBalanceRequest(userId, "ORDER-1", BigDecimal.TEN, "idem-1")
        );

        assertFalse(result.success());
        assertEquals(WalletContractErrorCode.WALLET_NOT_FOUND.name(), result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    void refundBalance_NoPaymentTransaction_ShouldReturnPaymentNotFoundError() {
        UUID userId = UUID.randomUUID();
        when(walletService.refundBalanceForOrder(userId, "ORDER-2", BigDecimal.TEN, "idem-2"))
                .thenThrow(new IllegalStateException("Successful payment transaction not found for orderId: ORDER-2"));

        WalletMutationResult result = contractService.refundBalance(
                new RefundBalanceRequest(userId, "ORDER-2", BigDecimal.TEN, "idem-2")
        );

        assertFalse(result.success());
        assertEquals(WalletContractErrorCode.PAYMENT_NOT_FOUND.name(), result.errorCode());
        assertFalse(result.retryable());
    }

    @Test
    void deductBalance_UnexpectedRuntimeException_ShouldReturnRetryableInternalError() {
        UUID userId = UUID.randomUUID();
        when(walletService.deductBalanceForOrder(userId, "ORDER-3", BigDecimal.TEN, "idem-3"))
                .thenThrow(new RuntimeException("temporary downstream failure"));

        WalletMutationResult result = contractService.deductBalance(
                new DeductBalanceRequest(userId, "ORDER-3", BigDecimal.TEN, "idem-3")
        );

        assertFalse(result.success());
        assertEquals(WalletContractErrorCode.INTERNAL_ERROR.name(), result.errorCode());
        assertTrue(result.retryable());
    }

    @Test
    void checkBalance_ShouldReflectWalletBalanceComparison() {
        UUID userId = UUID.randomUUID();
        when(walletService.getWallet(userId)).thenReturn(
                WalletResponse.builder()
                        .walletId(UUID.randomUUID())
                        .userId(userId)
                        .balance(new BigDecimal("100.00"))
                        .build()
        );

        CheckBalanceResult sufficient = contractService.checkBalance(
                new CheckBalanceRequest(userId, new BigDecimal("90.00"))
        );
        CheckBalanceResult insufficient = contractService.checkBalance(
                new CheckBalanceRequest(userId, new BigDecimal("120.00"))
        );

        assertTrue(sufficient.sufficient());
        assertFalse(insufficient.sufficient());
        assertEquals(new BigDecimal("100.00"), sufficient.currentBalance());
    }
}
