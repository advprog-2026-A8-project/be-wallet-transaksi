package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackProcessorTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private NoOpPaymentCallbackProcessor processor;

    @ParameterizedTest
    @MethodSource("paymentStatusScenarios")
    void process_StatusMapping_ShouldTriggerExpectedWalletHandling(
            String orderId,
            String statusCode,
            String grossAmount,
            String transactionStatus,
            boolean settlementExpected,
            boolean failureExpected
    ) {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setOrderId(orderId);
        request.setStatusCode(statusCode);
        request.setGrossAmount(grossAmount);
        request.setTransactionStatus(transactionStatus);

        processor.process(request);

        if (settlementExpected) {
            verify(walletService).handlePaymentSettlement(orderId);
        }
        if (failureExpected) {
            verify(walletService).handlePaymentFailure(orderId);
        }
    }

    @Test
    void process_StatusWithWhitespace_ShouldStillTriggerSettlementHandling() {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setOrderId("ORDER-3");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setTransactionStatus(" settlement ");

        processor.process(request);

        verify(walletService).handlePaymentSettlement("ORDER-3");
    }

    @Test
    void process_BlankOrderId_ShouldNotTriggerWalletService() {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setOrderId("   ");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setTransactionStatus("settlement");

        processor.process(request);

        verifyNoInteractions(walletService);
    }

    @Test
    void process_DuplicateSettlementCallback_ShouldTriggerWalletServiceOnlyOnce() {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setOrderId("ORDER-DUP-1");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setTransactionStatus("settlement");

        processor.process(request);
        processor.process(request);

        verify(walletService, times(1)).handlePaymentSettlement("ORDER-DUP-1");
    }

    @Test
    void process_SettlementThenDenySameOrder_ShouldIgnoreSecondTerminalCallback() {
        PaymentCallbackRequest settlement = new PaymentCallbackRequest();
        settlement.setOrderId("ORDER-TRANS-1");
        settlement.setStatusCode("200");
        settlement.setGrossAmount("10000.00");
        settlement.setTransactionStatus("settlement");

        PaymentCallbackRequest deny = new PaymentCallbackRequest();
        deny.setOrderId("ORDER-TRANS-1");
        deny.setStatusCode("202");
        deny.setGrossAmount("10000.00");
        deny.setTransactionStatus("deny");

        processor.process(settlement);
        processor.process(deny);

        verify(walletService, times(1)).handlePaymentSettlement("ORDER-TRANS-1");
        verify(walletService, never()).handlePaymentFailure("ORDER-TRANS-1");
    }

    @Test
    void process_SettlementThenPendingSameOrder_ShouldIgnorePendingFollowUpCallback() {
        PaymentCallbackRequest settlement = new PaymentCallbackRequest();
        settlement.setOrderId("ORDER-TRANS-2");
        settlement.setStatusCode("200");
        settlement.setGrossAmount("10000.00");
        settlement.setTransactionStatus("settlement");

        PaymentCallbackRequest pending = new PaymentCallbackRequest();
        pending.setOrderId("ORDER-TRANS-2");
        pending.setStatusCode("201");
        pending.setGrossAmount("10000.00");
        pending.setTransactionStatus("pending");

        processor.process(settlement);
        processor.process(pending);

        verify(walletService, times(1)).handlePaymentSettlement("ORDER-TRANS-2");
        verify(walletService, never()).handlePaymentFailure("ORDER-TRANS-2");
    }

    @Test
    void process_DuplicateSettlementWithDifferentStatusCase_ShouldTriggerWalletServiceOnlyOnce() {
        PaymentCallbackRequest first = new PaymentCallbackRequest();
        first.setOrderId("ORDER-DUP-CASE-1");
        first.setStatusCode("200");
        first.setGrossAmount("10000.00");
        first.setTransactionStatus("settlement");

        PaymentCallbackRequest second = new PaymentCallbackRequest();
        second.setOrderId("ORDER-DUP-CASE-1");
        second.setStatusCode("200");
        second.setGrossAmount("10000.00");
        second.setTransactionStatus("SETTLEMENT");

        processor.process(first);
        processor.process(second);

        verify(walletService, times(1)).handlePaymentSettlement("ORDER-DUP-CASE-1");
    }

    private static Stream<Arguments> paymentStatusScenarios() {
        return Stream.of(
                Arguments.of("ORDER-1", "200", "10000.00", "settlement", true, false),
                Arguments.of("ORDER-2", "202", "10000.00", "deny", false, true),
                Arguments.of("ORDER-CAPTURE-1", "200", "10000.00", "capture", true, false)
        );
    }
}
