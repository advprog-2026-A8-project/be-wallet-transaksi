package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletMutationRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.WalletNotFoundException;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID walletId;
    private WalletResponse walletResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        walletResponse = WalletResponse.builder()
                .walletId(walletId)
                .userId(userId)
                .balance(BigDecimal.valueOf(100.00))
                .build();
    }

    @Test
    void getWallet_Success() throws Exception {
        when(walletService.getWallet(userId)).thenReturn(walletResponse);

        mockMvc.perform(get("/wallet/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void getWallet_NotFound() throws Exception {
        when(walletService.getWallet(userId)).thenThrow(new WalletNotFoundException(userId));

        mockMvc.perform(get("/wallet/{userId}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWallet_Success() throws Exception {
        WalletResponse newWalletResponse = WalletResponse.builder()
                .walletId(walletId)
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();

        when(walletService.createWallet(userId)).thenReturn(newWalletResponse);

        mockMvc.perform(post("/wallet")
                        .param("userId", userId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void topUp_Success() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(50.00));

        WalletResponse updatedResponse = WalletResponse.builder()
                .walletId(walletId)
                .userId(userId)
                .balance(BigDecimal.valueOf(150.00))
                .build();

        when(walletService.topUp(any(TopUpRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void topUp_InvalidAmount() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(0));

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be at least 1"));
    }

    @Test
    void topUp_NullUserId() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(null);
        request.setAmount(BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void topUp_NullAmount() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(null);

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getTransactionHistory_Success() throws Exception {
        TransactionResponse latest = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .walletId(walletId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .description("Latest payment")
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0))
                .build();

        TransactionResponse older = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .walletId(walletId)
                .amount(BigDecimal.valueOf(20.00))
                .type(TransactionType.REFUND)
                .status(TransactionStatus.SUCCESS)
                .description("Older refund")
                .createdAt(LocalDateTime.of(2026, 3, 22, 9, 0))
                .build();

        when(walletService.getTransactionHistory(userId)).thenReturn(List.of(latest, older));

        mockMvc.perform(get("/wallet/{userId}/transactions", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Latest payment"))
                .andExpect(jsonPath("$[1].description").value("Older refund"));
    }

    @Test
    void getTransactionHistoryByStatus_Success() throws Exception {
        TransactionResponse failedWithdraw = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .walletId(walletId)
                .amount(BigDecimal.valueOf(40.00))
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.FAILED)
                .description("Withdraw failed")
                .createdAt(LocalDateTime.of(2026, 3, 22, 11, 0))
                .build();

        when(walletService.getTransactionHistoryByStatus(userId, TransactionStatus.FAILED))
                .thenReturn(List.of(failedWithdraw));

        mockMvc.perform(get("/wallet/{userId}/transactions", userId)
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].description").value("Withdraw failed"));
    }

    @Test
    void getTransactionHistory_WalletNotFound() throws Exception {
        when(walletService.getTransactionHistory(userId)).thenThrow(new WalletNotFoundException(userId));

        mockMvc.perform(get("/wallet/{userId}/transactions", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionHistory_InvalidStatus_BadRequest() throws Exception {
        mockMvc.perform(get("/wallet/{userId}/transactions", userId)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void pay_Success() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        when(walletService.pay(userId, BigDecimal.valueOf(50.00), "Order payment")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(50.00))
                        .build()
        );

        mockMvc.perform(post("/wallet/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void refund_Success() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order refund", BigDecimal.valueOf(25.00));

        when(walletService.refund(userId, BigDecimal.valueOf(25.00), "Order refund")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(125.00))
                        .build()
        );

        mockMvc.perform(post("/wallet/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.00));
    }

    @Test
    void withdraw_Success() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(30.00));

        when(walletService.withdraw(userId, BigDecimal.valueOf(30.00), "BCA-123456")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(70.00))
                        .build()
        );

        mockMvc.perform(post("/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void pay_InsufficientBalance_BadRequest() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(500.00));

        when(walletService.pay(userId, BigDecimal.valueOf(500.00), "Order payment"))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        mockMvc.perform(post("/wallet/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdraw_InsufficientBalance_BadRequest() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(500.00));

        when(walletService.withdraw(userId, BigDecimal.valueOf(500.00), "BCA-123456"))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        mockMvc.perform(post("/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private WalletMutationRequest buildMutationRequest(String description, BigDecimal amount) {
        WalletMutationRequest request = new WalletMutationRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        request.setDescription(description);
        return request;
    }
}
