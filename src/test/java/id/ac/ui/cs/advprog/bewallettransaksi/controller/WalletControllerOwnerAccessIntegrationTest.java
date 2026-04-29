package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletMutationRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerOwnerAccessIntegrationTest {
    private static final String JWT_SECRET = "DefaultSecretKeyUntukDevelopmentLokalYangSangatPanjangSekali123!@#";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;
    @MockitoBean
    private UsernameToUserIdResolver usernameToUserIdResolver;

    private UUID ownerUserId;
    private WalletResponse walletResponse;
    private TransactionResponse transactionResponse;
    private WalletMutationRequest mutationRequest;

    @BeforeEach
    void setUp() {
        ownerUserId = UUID.randomUUID();
        walletResponse = WalletResponse.builder()
                .walletId(UUID.randomUUID())
                .userId(ownerUserId)
                .balance(BigDecimal.valueOf(100.00))
                .build();
        transactionResponse = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .walletId(walletResponse.getWalletId())
                .amount(BigDecimal.valueOf(10.00))
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .description("sample")
                .build();
        mutationRequest = new WalletMutationRequest();
        mutationRequest.setUserId(ownerUserId);
        mutationRequest.setAmount(BigDecimal.valueOf(10.00));
        mutationRequest.setDescription("payment");
        when(usernameToUserIdResolver.resolve(any())).thenReturn(java.util.Optional.empty());
        when(usernameToUserIdResolver.resolve("owner_username")).thenReturn(java.util.Optional.of(ownerUserId));
    }

    @Test
    void getWallet_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        when(walletService.getWallet(ownerUserId)).thenReturn(walletResponse);

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "TITIPER");
        mockMvc.perform(get("/wallet/{userId}", ownerUserId)
                        .header("Authorization", "Bearer " + differentUserJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void getWallet_SignedJwtOfOwner_ShouldReturnSuccess() throws Exception {
        when(walletService.getWallet(ownerUserId)).thenReturn(walletResponse);

        String ownerJwt = generateJwtToken(ownerUserId.toString(), "TITIPER");
        mockMvc.perform(get("/wallet/{userId}", ownerUserId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ownerUserId.toString()));
    }

    @Test
    void getWallet_UsernameSubjectJwtOfOwner_ShouldReturnSuccess() throws Exception {
        when(walletService.getWallet(ownerUserId)).thenReturn(walletResponse);

        String ownerJwt = generateJwtToken("owner_username", "TITIPER");
        mockMvc.perform(get("/wallet/{userId}", ownerUserId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ownerUserId.toString()));
    }

    @Test
    void getWallet_UsernameSubjectJwtWithoutResolverMapping_ShouldReturnForbidden() throws Exception {
        when(walletService.getWallet(ownerUserId)).thenReturn(walletResponse);

        String ownerJwt = generateJwtToken("unknown_username", "TITIPER");
        mockMvc.perform(get("/wallet/{userId}", ownerUserId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void getWallet_UnsupportedRoleJwt_ShouldReturnForbidden() throws Exception {
        when(walletService.getWallet(ownerUserId)).thenReturn(walletResponse);

        String unsupportedRoleJwt = generateJwtToken(ownerUserId.toString(), "CUSTOMER");
        mockMvc.perform(get("/wallet/{userId}", ownerUserId)
                        .header("Authorization", "Bearer " + unsupportedRoleJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void createWallet_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        when(walletService.createWallet(ownerUserId)).thenReturn(walletResponse);

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "TITIPER");
        mockMvc.perform(post("/wallet")
                        .header("Authorization", "Bearer " + differentUserJwt)
                        .param("userId", ownerUserId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void createWallet_InvalidJwt_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/wallet")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .param("userId", ownerUserId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void createWallet_LegacyReadToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/wallet")
                        .header("Authorization", "Bearer valid-read-jwt")
                        .param("userId", ownerUserId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void createWallet_UnsupportedRoleJwt_ShouldReturnForbidden() throws Exception {
        when(walletService.createWallet(ownerUserId)).thenReturn(walletResponse);

        String unsupportedRoleJwt = generateJwtToken(ownerUserId.toString(), "CUSTOMER");
        mockMvc.perform(post("/wallet")
                        .header("Authorization", "Bearer " + unsupportedRoleJwt)
                        .param("userId", ownerUserId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void createWallet_AdminJwtOfDifferentUser_ShouldReturnCreated() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        WalletResponse otherWalletResponse = WalletResponse.builder()
                .walletId(UUID.randomUUID())
                .userId(otherUserId)
                .balance(BigDecimal.ZERO)
                .build();
        when(walletService.createWallet(otherUserId)).thenReturn(otherWalletResponse);

        String adminJwt = generateJwtToken(ownerUserId.toString(), "ADMIN");
        mockMvc.perform(post("/wallet")
                        .header("Authorization", "Bearer " + adminJwt)
                        .param("userId", otherUserId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(otherUserId.toString()));
    }

    @Test
    void getTransactionHistory_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        when(walletService.getTransactionHistory(ownerUserId)).thenReturn(List.of(transactionResponse));

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "TITIPER");
        mockMvc.perform(get("/wallet/{userId}/transactions", ownerUserId)
                        .header("Authorization", "Bearer " + differentUserJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void pay_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        when(walletService.pay(ownerUserId, BigDecimal.valueOf(10.00), "payment"))
                .thenReturn(walletResponse);

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "TITIPER");
        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer " + differentUserJwt)
                        .header("Idempotency-Key", "idem-owner-forbidden")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void refund_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        when(walletService.refund(ownerUserId, BigDecimal.valueOf(10.00), "payment"))
                .thenReturn(walletResponse);

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "TITIPER");
        mockMvc.perform(post("/wallet/refund")
                        .header("Authorization", "Bearer " + differentUserJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void topUp_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(ownerUserId);
        request.setAmount(BigDecimal.valueOf(10.00));
        when(walletService.topUp(request)).thenReturn(walletResponse);

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "TITIPER");
        mockMvc.perform(post("/wallet/topup")
                        .header("Authorization", "Bearer " + differentUserJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void getTransactionHistory_LegacyReadToken_ShouldReturnUnauthorized() throws Exception {
        when(walletService.getTransactionHistory(ownerUserId)).thenReturn(List.of(transactionResponse));

        mockMvc.perform(get("/wallet/{userId}/transactions", ownerUserId)
                        .header("Authorization", "Bearer valid-read-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void getWallet_LegacyReadToken_ShouldReturnUnauthorized() throws Exception {
        when(walletService.getWallet(ownerUserId)).thenReturn(walletResponse);

        mockMvc.perform(get("/wallet/{userId}", ownerUserId)
                        .header("Authorization", "Bearer valid-read-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void getTransactionHistory_UnsupportedRoleJwt_ShouldReturnForbidden() throws Exception {
        when(walletService.getTransactionHistory(ownerUserId)).thenReturn(List.of(transactionResponse));

        String unsupportedRoleJwt = generateJwtToken(ownerUserId.toString(), "CUSTOMER");
        mockMvc.perform(get("/wallet/{userId}/transactions", ownerUserId)
                        .header("Authorization", "Bearer " + unsupportedRoleJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void pay_AdminJwt_ShouldReturnSuccess() throws Exception {
        when(walletService.pay(eq(ownerUserId), any(BigDecimal.class), eq("payment")))
                .thenReturn(walletResponse);

        String adminJwt = generateJwtToken(ownerUserId.toString(), "ADMIN");
        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer " + adminJwt)
                        .header("Idempotency-Key", "idem-owner-admin")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ownerUserId.toString()));
    }

    @Test
    void pay_UnsupportedRoleJwt_ShouldReturnForbidden() throws Exception {
        when(walletService.pay(eq(ownerUserId), any(BigDecimal.class), eq("payment")))
                .thenReturn(walletResponse);

        String unsupportedRoleJwt = generateJwtToken(ownerUserId.toString(), "CUSTOMER");
        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer " + unsupportedRoleJwt)
                        .header("Idempotency-Key", "idem-owner-unsupported-role")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void pay_LegacyReadToken_ShouldReturnUnauthorized() throws Exception {
        when(walletService.pay(eq(ownerUserId), any(BigDecimal.class), eq("payment")))
                .thenReturn(walletResponse);

        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer valid-read-jwt")
                        .header("Idempotency-Key", "idem-owner-legacy-read")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void pay_LegacyAcceptedBearerToken_ShouldReturnUnauthorized() throws Exception {
        when(walletService.pay(eq(ownerUserId), any(BigDecimal.class), eq("payment")))
                .thenReturn(walletResponse);

        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer test-token")
                        .header("Idempotency-Key", "idem-owner-legacy-token")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void pay_SameIdempotencyKeyAfterFirstFailure_ShouldAllowRetrySuccess() throws Exception {
        doReturn(walletResponse)
                .when(walletService).pay(eq(ownerUserId), eq(BigDecimal.valueOf(10.00)), eq("payment"));
        when(walletService.pay(eq(ownerUserId), eq(BigDecimal.valueOf(9999.00)), eq("payment")))
                .thenThrow(new IllegalStateException("Insufficient balance"));

        String ownerJwt = generateJwtToken(ownerUserId.toString(), "TITIPER");

        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .header("Idempotency-Key", "idem-retry-int-001")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":9999.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/wallet/pay")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .header("Idempotency-Key", "idem-retry-int-001")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isOk());
    }

    @Test
    void refund_UnsupportedRoleJwt_ShouldReturnForbidden() throws Exception {
        when(walletService.refund(ownerUserId, BigDecimal.valueOf(10.00), "payment"))
                .thenReturn(walletResponse);

        String unsupportedRoleJwt = generateJwtToken(ownerUserId.toString(), "CUSTOMER");
        mockMvc.perform(post("/wallet/refund")
                        .header("Authorization", "Bearer " + unsupportedRoleJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void refund_LegacyReadToken_ShouldReturnUnauthorized() throws Exception {
        when(walletService.refund(ownerUserId, BigDecimal.valueOf(10.00), "payment"))
                .thenReturn(walletResponse);

        mockMvc.perform(post("/wallet/refund")
                        .header("Authorization", "Bearer valid-read-jwt")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"payment"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void topUp_UnsupportedRoleJwt_ShouldReturnForbidden() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(ownerUserId);
        request.setAmount(BigDecimal.valueOf(10.00));
        when(walletService.topUp(request)).thenReturn(walletResponse);

        String unsupportedRoleJwt = generateJwtToken(ownerUserId.toString(), "CUSTOMER");
        mockMvc.perform(post("/wallet/topup")
                        .header("Authorization", "Bearer " + unsupportedRoleJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void topUp_JastiperJwtOfOwner_ShouldReturnForbidden() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(ownerUserId);
        request.setAmount(BigDecimal.valueOf(10.00));
        when(walletService.topUp(request)).thenReturn(walletResponse);

        String jastiperJwt = generateJwtToken(ownerUserId.toString(), "JASTIPER");
        mockMvc.perform(post("/wallet/topup")
                        .header("Authorization", "Bearer " + jastiperJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void topUp_AdminJwtWithForgedJastiperHeader_ShouldReturnSuccess() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(ownerUserId);
        request.setAmount(BigDecimal.valueOf(10.00));
        when(walletService.topUp(any(TopUpRequest.class))).thenReturn(walletResponse);

        String adminJwt = generateJwtToken(ownerUserId.toString(), "ADMIN");
        mockMvc.perform(post("/wallet/topup")
                        .header("Authorization", "Bearer " + adminJwt)
                        .header("X-Role", "JASTIPER")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00}
                                """.formatted(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ownerUserId.toString()));
    }

    @Test
    void withdraw_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        when(walletService.withdraw(ownerUserId, BigDecimal.valueOf(10.00), "bank-account"))
                .thenReturn(walletResponse);

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "JASTIPER");
        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer " + differentUserJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"bank-account"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void withdraw_AdminJwt_ShouldReturnSuccess() throws Exception {
        when(walletService.withdraw(eq(ownerUserId), any(BigDecimal.class), eq("bank-account")))
                .thenReturn(walletResponse);

        String adminJwt = generateJwtToken(ownerUserId.toString(), "ADMIN");
        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"bank-account"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ownerUserId.toString()));
    }

    @Test
    void withdraw_UnsupportedRoleJwt_ShouldReturnForbidden() throws Exception {
        when(walletService.withdraw(eq(ownerUserId), any(BigDecimal.class), eq("bank-account")))
                .thenReturn(walletResponse);

        String unsupportedRoleJwt = generateJwtToken(ownerUserId.toString(), "CUSTOMER");
        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer " + unsupportedRoleJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"bank-account"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void withdraw_TitiperJwtWithForgedJastiperHeader_ShouldReturnForbidden() throws Exception {
        when(walletService.withdraw(eq(ownerUserId), any(BigDecimal.class), eq("bank-account")))
                .thenReturn(walletResponse);

        String titiperJwt = generateJwtToken(ownerUserId.toString(), "TITIPER");
        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer " + titiperJwt)
                        .header("X-Role", "JASTIPER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"bank-account"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
    }

    @Test
    void withdraw_LegacyReadToken_ShouldReturnUnauthorized() throws Exception {
        when(walletService.withdraw(eq(ownerUserId), any(BigDecimal.class), eq("bank-account")))
                .thenReturn(walletResponse);

        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer valid-read-jwt")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"bank-account"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void withdraw_NonJwtBearerWithLegacyJastiperHeader_ShouldReturnUnauthorized() throws Exception {
        when(walletService.withdraw(eq(ownerUserId), any(BigDecimal.class), eq("bank-account")))
                .thenReturn(walletResponse);

        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer legacy-token")
                        .header("X-Role", "JASTIPER")
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"bank-account"}
                                """.formatted(ownerUserId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan!"));
    }

    @Test
    void withdraw_AdminJwtOfDifferentUser_ShouldReturnSuccess() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        WalletResponse otherWalletResponse = WalletResponse.builder()
                .walletId(UUID.randomUUID())
                .userId(otherUserId)
                .balance(BigDecimal.valueOf(100.00))
                .build();
        when(walletService.withdraw(eq(otherUserId), any(BigDecimal.class), eq("bank-account")))
                .thenReturn(otherWalletResponse);

        String adminJwt = generateJwtToken(ownerUserId.toString(), "ADMIN");
        mockMvc.perform(post("/wallet/withdraw")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType("application/json")
                        .content("""
                                {"userId":"%s","amount":10.00,"description":"bank-account"}
                                """.formatted(otherUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(otherUserId.toString()));
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
