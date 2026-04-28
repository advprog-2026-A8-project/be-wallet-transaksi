package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private UUID ownerUserId;
    private WalletResponse walletResponse;
    private TransactionResponse transactionResponse;

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
    void getTransactionHistory_SignedJwtOfDifferentUser_ShouldReturnForbidden() throws Exception {
        when(walletService.getTransactionHistory(ownerUserId)).thenReturn(List.of(transactionResponse));

        String differentUserJwt = generateJwtToken(UUID.randomUUID().toString(), "TITIPER");
        mockMvc.perform(get("/wallet/{userId}/transactions", ownerUserId)
                        .header("Authorization", "Bearer " + differentUserJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Akses ditolak!"));
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
