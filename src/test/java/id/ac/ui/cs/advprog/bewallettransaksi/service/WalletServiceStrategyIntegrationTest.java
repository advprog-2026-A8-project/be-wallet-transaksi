package id.ac.ui.cs.advprog.bewallettransaksi.service;

import id.ac.ui.cs.advprog.bewallettransaksi.config.WalletMetricsRecorder;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.TransactionRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.repository.WalletRepository;
import id.ac.ui.cs.advprog.bewallettransaksi.service.strategy.WalletMutationStrategy;
import id.ac.ui.cs.advprog.bewallettransaksi.service.strategy.WalletMutationStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceStrategyIntegrationTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletMutationStrategyResolver strategyResolver;

    @Mock
    private WalletMutationStrategy strategy;

    @Mock
    private OrderPaymentStatusPublisher orderPaymentStatusPublisher;

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @Mock
    private WalletMetricsRecorder walletMetricsRecorder;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID userId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.valueOf(100.00));
    }

    @Test
    void topUp_ShouldUseResolvedCreditStrategy() {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(25.00));

        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(strategyResolver.resolve(TransactionType.TOPUP)).thenReturn(strategy);
        when(strategy.apply(BigDecimal.valueOf(100.00), BigDecimal.valueOf(25.00)))
                .thenReturn(BigDecimal.valueOf(125.00));

        walletService.topUp(request);

        verify(strategyResolver).resolve(TransactionType.TOPUP);
        verify(strategy).apply(BigDecimal.valueOf(100.00), BigDecimal.valueOf(25.00));
    }

    @Test
    void pay_ShouldUseResolvedDebitStrategy() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(strategyResolver.resolve(TransactionType.PAYMENT)).thenReturn(strategy);
        when(strategy.apply(BigDecimal.valueOf(100.00), BigDecimal.valueOf(30.00)))
                .thenReturn(BigDecimal.valueOf(70.00));

        walletService.pay(userId, BigDecimal.valueOf(30.00), "Order payment");

        verify(strategyResolver).resolve(TransactionType.PAYMENT);
        verify(strategy).apply(BigDecimal.valueOf(100.00), BigDecimal.valueOf(30.00));
    }
}
