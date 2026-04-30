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
}
