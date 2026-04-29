package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest {
    private static final String JWT_SECRET = "DefaultSecretKeyUntukDevelopmentLokalYangSangatPanjangSekali123!@#";
    private static final String AUTH_HEADER = "Authorization";
    private static final String READ_JWT_HEADER_VALUE = "Bearer valid-read-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;
    @MockitoBean
    private WalletRequestAccessPolicy walletRequestAccessPolicy;

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

        stubAccessPolicyDefaults();
    }

    private void stubAccessPolicyDefaults() {
        when(walletRequestAccessPolicy.isOwnerMismatchToken(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isForbiddenTopUpRole(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isInvalidJwtToken(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isDisallowedRoleForPay(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isValidReadJwt(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isValidJastiperJwt(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isAllowedPayRole(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isAllowedWalletMutationRole(anyString())).thenReturn(true);
        when(walletRequestAccessPolicy.isJwtBearerToken(anyString())).thenReturn(true);
        when(walletRequestAccessPolicy.isOwnerMismatchToken(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isForbiddenTopUpRole(isNull())).thenReturn(false);
        when(walletRequestAccessPolicy.isForbiddenTopUpRole(anyString())).thenReturn(false);
        when(walletRequestAccessPolicy.isForbiddenTopUpRole(isNull())).thenReturn(false);
        when(walletRequestAccessPolicy.isInvalidJwtToken(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isDisallowedRoleForPay(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isValidReadJwt(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isValidJastiperJwt(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isAllowedPayRole(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isAllowedWalletMutationRole(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isJwtBearerToken(null)).thenReturn(false);
        when(walletRequestAccessPolicy.isOwnerMismatchToken("Bearer valid-non-admin-other-user")).thenReturn(true);
        when(walletRequestAccessPolicy.isForbiddenTopUpRole("Bearer valid-jastiper")).thenReturn(true);
        when(walletRequestAccessPolicy.isInvalidJwtToken("Bearer invalid.jwt.token")).thenReturn(true);
        when(walletRequestAccessPolicy.isDisallowedRoleForPay("Bearer valid-jastiper-jwt")).thenReturn(true);
        when(walletRequestAccessPolicy.isValidReadJwt("Bearer valid-read-jwt")).thenReturn(true);
        when(walletRequestAccessPolicy.isValidReadJwt("Bearer valid-jastiper-jwt")).thenReturn(true);
        when(walletRequestAccessPolicy.isValidJastiperJwt("Bearer valid-jastiper-jwt")).thenReturn(true);
        when(walletRequestAccessPolicy.isAllowedPayRole("Bearer valid-read-jwt")).thenReturn(true);
        when(walletRequestAccessPolicy.isAllowedPayRole("Bearer valid-jastiper-jwt")).thenReturn(false);
        when(walletRequestAccessPolicy.isAllowedPayRole("Bearer test-token")).thenReturn(false);
        when(walletRequestAccessPolicy.isAllowedWalletMutationRole("Bearer invalid.jwt.token")).thenReturn(false);
        when(walletRequestAccessPolicy.isJwtBearerToken("Bearer test-token")).thenReturn(true);
        when(walletRequestAccessPolicy.isJwtBearerToken("Bearer invalid.jwt.token")).thenReturn(true);
    }

    @Test
    void getWallet_Success() throws Exception {
        when(walletService.getWallet(userId)).thenReturn(walletResponse);

        mockMvc.perform(get("/wallet/{userId}", userId)
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void getWallet_MissingJwt_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        when(walletService.getWallet(userId)).thenReturn(walletResponse);

        mockMvc.perform(get("/wallet/{userId}", userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getWallet_NonAdminOwnerMismatch_ShouldReturnForbidden() throws Exception {
        when(walletService.getWallet(userId)).thenReturn(walletResponse);

        mockMvc.perform(get("/wallet/{userId}", userId)
                        .header("Authorization", "Bearer valid-non-admin-other-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getWallet_InvalidJwtToken_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        when(walletService.getWallet(userId)).thenReturn(walletResponse);

        mockMvc.perform(get("/wallet/{userId}", userId)
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getWallet_NotFound() throws Exception {
        when(walletService.getWallet(userId)).thenThrow(new WalletNotFoundException(userId));

        mockMvc.perform(get("/wallet/{userId}", userId)
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE))
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
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .param("userId", userId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void createWallet_MissingJwt_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        mockMvc.perform(post("/wallet")
                        .param("userId", userId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void createWallet_DuplicateUser_BadRequest() throws Exception {
        when(walletService.createWallet(userId))
                .thenThrow(new DataIntegrityViolationException("duplicate userId"));

        mockMvc.perform(post("/wallet")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .param("userId", userId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createWallet_MissingUserId_BadRequestWithStandardErrorBody() throws Exception {
        mockMvc.perform(post("/wallet"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createWallet_MissingUserId_BadRequestWithFriendlyMessage() throws Exception {
        mockMvc.perform(post("/wallet"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required request parameter: userId"));
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
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void topUp_MissingJwt_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void topUp_LegacyJastiperToken_ShouldReturnUnauthorized() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/topup")
                        .header("Authorization", "Bearer valid-jastiper")
                        .header("X-Role", "JASTIPER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void topUp_ValidSignedJastiperJwtWithoutLegacyRoleHeader_ShouldReturnForbidden() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(50.00));

        String jwt = generateJwtToken("jastiper-subject", "JASTIPER");
        when(walletRequestAccessPolicy.isForbiddenTopUpRole("Bearer " + jwt)).thenReturn(true);
        when(walletRequestAccessPolicy.isValidReadJwt("Bearer " + jwt)).thenReturn(true);
        mockMvc.perform(post("/wallet/topup")
                        .header(AUTH_HEADER, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void topUp_InvalidAmount() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(0));

        mockMvc.perform(post("/wallet/topup")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be at least 1"));
    }

    @Test
    void topUp_NullUserId() throws Exception {
        TopUpRequest request = buildTopUpRequest(null, BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/topup")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void topUp_NullUserId_BadRequestWithConsistentMessage() throws Exception {
        TopUpRequest request = buildTopUpRequest(null, BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/topup")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User ID must not be null"));
    }

    @Test
    void topUp_NullAmount_BadRequestWithConsistentMessage() throws Exception {
        TopUpRequest request = buildTopUpRequest(userId, null);

        mockMvc.perform(post("/wallet/topup")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be at least 1"));
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

        mockMvc.perform(get("/wallet/{userId}/transactions", userId)
                        .header("Authorization", "Bearer valid-read-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Latest payment"))
                .andExpect(jsonPath("$[1].description").value("Older refund"));
    }

    @Test
    void getTransactionHistory_MissingJwt_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        mockMvc.perform(get("/wallet/{userId}/transactions", userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
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
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].description").value("Withdraw failed"));
    }

    @Test
    void getTransactionHistoryByStatus_LowercaseParam_Success() throws Exception {
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
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .param("status", "failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILED"));
    }

    @Test
    void getTransactionHistory_WalletNotFound() throws Exception {
        when(walletService.getTransactionHistory(userId)).thenThrow(new WalletNotFoundException(userId));

        mockMvc.perform(get("/wallet/{userId}/transactions", userId)
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransactionHistory_InvalidStatus_BadRequest() throws Exception {
        mockMvc.perform(get("/wallet/{userId}/transactions", userId)
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getTransactionHistory_EmptyStatusParam_ShouldFallbackToAllHistory() throws Exception {
        TransactionResponse latest = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .walletId(walletId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .description("Latest payment")
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0))
                .build();

        when(walletService.getTransactionHistory(userId)).thenReturn(List.of(latest));

        mockMvc.perform(get("/wallet/{userId}/transactions", userId)
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .param("status", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Latest payment"));
    }

    @Test
    void getTransactionHistory_WhitespaceStatusParam_ShouldFallbackToAllHistory() throws Exception {
        TransactionResponse latest = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .walletId(walletId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .description("Latest payment")
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0))
                .build();

        when(walletService.getTransactionHistory(userId)).thenReturn(List.of(latest));

        mockMvc.perform(get("/wallet/{userId}/transactions", userId)
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .param("status", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Latest payment"));
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
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .header("Idempotency-Key", "idem-pay-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void pay_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pay_Unauthenticated_ShouldReturnUnauthorizedWithMessage() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void pay_InvalidBearerToken_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer invalid.token.value")
                        .header("Idempotency-Key", "idem-pay-invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void pay_ValidJwtButDisallowedRole_ShouldReturnForbiddenWithApiResponse() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer valid-jastiper-jwt")
                        .header("Idempotency-Key", "idem-pay-forbidden-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void pay_ValidSignedTitiperJwt_ShouldSucceed() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        when(walletService.pay(userId, BigDecimal.valueOf(50.00), "Order payment")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(50.00))
                        .build()
        );

        String jwt = generateJwtToken("titiper-subject", "TITIPER");
        when(walletRequestAccessPolicy.isValidTitiperJwt("Bearer " + jwt)).thenReturn(true);
        when(walletRequestAccessPolicy.isAllowedPayRole("Bearer " + jwt)).thenReturn(true);
        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer " + jwt)
                        .header("Idempotency-Key", "idem-pay-valid-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void pay_MissingIdempotencyKey_ShouldReturnBadRequest() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        when(walletService.pay(userId, BigDecimal.valueOf(50.00), "Order payment")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(50.00))
                        .build()
        );

        mockMvc.perform(post("/wallet/pay")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required header: Idempotency-Key"));
    }

    @Test
    void pay_DuplicateIdempotencyKey_ShouldReturnConflict() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(50.00));

        when(walletService.pay(userId, BigDecimal.valueOf(50.00), "Order payment")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(50.00))
                        .build()
        );

        mockMvc.perform(post("/wallet/pay")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .header("Idempotency-Key", "idem-dup-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/wallet/pay")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .header("Idempotency-Key", "idem-dup-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Duplicate idempotency key"));

        verify(walletService, times(1)).pay(userId, BigDecimal.valueOf(50.00), "Order payment");
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
                        .header("Authorization", "Bearer valid-read-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.00));
    }

    @Test
    void refund_MissingJwt_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order refund", BigDecimal.valueOf(25.00));

        mockMvc.perform(post("/wallet/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void refund_NullUserId_BadRequestWithConsistentMessage() throws Exception {
        WalletMutationRequest request = new WalletMutationRequest();
        request.setUserId(null);
        request.setAmount(BigDecimal.valueOf(25.00));
        request.setDescription("Order refund");

        mockMvc.perform(post("/wallet/refund")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User ID must not be null"));
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
                        .header("Authorization", "Bearer valid-jastiper-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void withdraw_ValidJastiperJwtWithoutLegacyRoleHeader_ShouldSucceed() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(30.00));

        when(walletService.withdraw(userId, BigDecimal.valueOf(30.00), "BCA-123456")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(70.00))
                        .build()
        );

        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer valid-jastiper-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void withdraw_ValidSignedJastiperJwtWithoutLegacyRoleHeader_ShouldSucceed() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(30.00));

        when(walletService.withdraw(userId, BigDecimal.valueOf(30.00), "BCA-123456")).thenReturn(
                WalletResponse.builder()
                        .walletId(walletId)
                        .userId(userId)
                        .balance(BigDecimal.valueOf(70.00))
                        .build()
        );

        String jwt = generateJwtToken("jastiper-subject", "JASTIPER");
        when(walletRequestAccessPolicy.isValidJastiperJwt("Bearer " + jwt)).thenReturn(true);
        when(walletRequestAccessPolicy.isValidReadJwt("Bearer " + jwt)).thenReturn(true);
        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void withdraw_BlankDescription_BadRequestWithConsistentMessage() throws Exception {
        WalletMutationRequest request = buildMutationRequest("   ", BigDecimal.valueOf(30.00));

        mockMvc.perform(post("/wallet/withdraw")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .header("X-Role", "JASTIPER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Description must not be blank"));
    }

    @Test
    void pay_InsufficientBalance_BadRequest() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", BigDecimal.valueOf(500.00));

        when(walletService.pay(userId, BigDecimal.valueOf(500.00), "Order payment"))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        mockMvc.perform(post("/wallet/pay")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .header("Idempotency-Key", "idem-pay-decimal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pay_AmountWithMoreThanTwoDecimals_BadRequest() throws Exception {
        WalletMutationRequest request = buildMutationRequest("Order payment", new BigDecimal("1.001"));

        mockMvc.perform(post("/wallet/pay")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .header("Idempotency-Key", "idem-pay-blank-desc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pay_BlankDescription_BadRequestWithConsistentMessage() throws Exception {
        WalletMutationRequest request = buildMutationRequest("   ", BigDecimal.valueOf(50.00));

        mockMvc.perform(post("/wallet/pay")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Description must not be blank"));
    }

    @Test
    void withdraw_InsufficientBalance_BadRequest() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(500.00));

        when(walletService.withdraw(userId, BigDecimal.valueOf(500.00), "BCA-123456"))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer valid-jastiper-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdraw_NonJastiperRole_ShouldReturnForbidden() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(30.00));

        mockMvc.perform(post("/wallet/withdraw")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void withdraw_NonJastiperRole_ShouldReturnForbiddenWithMessage() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(30.00));

        mockMvc.perform(post("/wallet/withdraw")
                        .header(AUTH_HEADER, READ_JWT_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void withdraw_MissingRoleHeader_ShouldReturnForbiddenWithExplicitMessage() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(30.00));

        mockMvc.perform(post("/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void withdraw_MissingJwt_ShouldReturnUnauthorizedWithApiResponse() throws Exception {
        WalletMutationRequest request = buildMutationRequest("BCA-123456", BigDecimal.valueOf(30.00));

        mockMvc.perform(post("/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private WalletMutationRequest buildMutationRequest(String description, BigDecimal amount) {
        WalletMutationRequest request = new WalletMutationRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        request.setDescription(description);
        return request;
    }

    private TopUpRequest buildTopUpRequest(UUID requestUserId, BigDecimal amount) {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(requestUserId);
        request.setAmount(amount);
        return request;
    }

    private String generateJwtToken(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}

