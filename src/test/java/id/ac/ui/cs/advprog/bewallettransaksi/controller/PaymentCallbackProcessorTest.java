package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.PaymentCallbackRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void process_SettlementStatus_ShouldTriggerPaymentSettlementHandling() {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setOrderId("ORDER-1");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setTransactionStatus("settlement");

        processor.process(request);

        verify(walletService).handlePaymentSettlement("ORDER-1");
    }

    @Test
    void process_DenyStatus_ShouldTriggerPaymentFailureHandling() {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setOrderId("ORDER-2");
        request.setStatusCode("202");
        request.setGrossAmount("10000.00");
        request.setTransactionStatus("deny");

        processor.process(request);

        verify(walletService).handlePaymentFailure("ORDER-2");
    }

    @Test
    void process_CaptureStatus_ShouldTriggerPaymentSettlementHandling() {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setOrderId("ORDER-CAPTURE-1");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setTransactionStatus("capture");

        processor.process(request);

        verify(walletService).handlePaymentSettlement("ORDER-CAPTURE-1");
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
}
