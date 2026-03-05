package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void topUp_NullUserId() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(null);
        request.setAmount(BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topUp_NullAmount() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(null);

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
