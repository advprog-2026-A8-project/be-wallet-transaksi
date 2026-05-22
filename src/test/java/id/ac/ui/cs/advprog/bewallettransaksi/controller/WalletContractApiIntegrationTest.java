package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceResult;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.OrderWalletContractService;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.WalletMutationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletContractApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderWalletContractService orderWalletContractService;
    @MockitoBean
    private WalletRequestAccessPolicy walletRequestAccessPolicy;

    @BeforeEach
    void setUp() {
        when(walletRequestAccessPolicy.isJwtBearerToken("Bearer valid-order-jwt")).thenReturn(true);
        when(walletRequestAccessPolicy.isValidReadJwt("Bearer valid-order-jwt")).thenReturn(true);
        when(walletRequestAccessPolicy.isAllowedWalletMutationRole("Bearer valid-order-jwt")).thenReturn(true);
    }

    @Test
    void checkBalance_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/contracts/wallet/check-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"amount\":10000.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkBalance_WithMalformedJwt_ShouldReturnUnauthorized() throws Exception {
        when(walletRequestAccessPolicy.isJwtBearerToken("Bearer malformed")).thenReturn(false);

        mockMvc.perform(post("/api/contracts/wallet/check-balance")
                        .header("Authorization", "Bearer malformed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"amount\":10000.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkBalance_WithDisallowedRole_ShouldReturnForbidden() throws Exception {
        when(walletRequestAccessPolicy.isAllowedWalletMutationRole("Bearer valid-order-jwt")).thenReturn(false);

        mockMvc.perform(post("/api/contracts/wallet/check-balance")
                        .header("Authorization", "Bearer valid-order-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"amount\":10000.00}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkBalance_WithValidJwt_ShouldReturnResult() throws Exception {
        when(orderWalletContractService.checkBalance(any()))
                .thenReturn(new CheckBalanceResult(true, new BigDecimal("120000.00")));

        mockMvc.perform(post("/api/contracts/wallet/check-balance")
                        .header("Authorization", "Bearer valid-order-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"amount\":10000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedBalance").value(120000.00));
    }

    @Test
    void deduct_WithValidJwt_ShouldReturnResult() throws Exception {
        when(orderWalletContractService.deductBalance(any()))
                .thenReturn(new WalletMutationResult(true, new BigDecimal("70000.00"), null));

        mockMvc.perform(post("/api/contracts/wallet/deduct")
                        .header("Authorization", "Bearer valid-order-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"00000000-0000-0000-0000-000000000001",
                                  "orderId":"ORDER-API-001",
                                  "amount":50000.00,
                                  "idempotencyKey":"idem-api-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedBalance").value(70000.00));
    }

    @Test
    void refund_WithValidJwt_ShouldReturnResult() throws Exception {
        when(orderWalletContractService.refundBalance(any()))
                .thenReturn(new WalletMutationResult(true, new BigDecimal("120000.00"), null));

        mockMvc.perform(post("/api/contracts/wallet/refund")
                        .header("Authorization", "Bearer valid-order-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"00000000-0000-0000-0000-000000000001",
                                  "orderId":"ORDER-API-001",
                                  "amount":50000.00,
                                  "idempotencyKey":"idem-refund-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.updatedBalance").value(120000.00));
    }
}

