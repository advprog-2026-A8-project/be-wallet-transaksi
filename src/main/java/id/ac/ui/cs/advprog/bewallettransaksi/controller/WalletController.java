package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import id.ac.ui.cs.advprog.bewallettransaksi.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletMutationRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.dto.WalletResponse;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.ForbiddenException;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.ConflictException;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallet")
public class WalletController {
    private static final String UNAUTHORIZED_MESSAGE = "Autentikasi diperlukan!";
    private static final String FORBIDDEN_MESSAGE = "Akses ditolak!";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String SIGNATURE_HEADER = "X-Signature-Key";
    private static final String CALLBACK_ACCEPTED_MESSAGE = "Callback accepted";
    private static final String INVALID_CALLBACK_SIGNATURE_MESSAGE = "Invalid callback signature";
    private static final String DUPLICATE_IDEMPOTENCY_MESSAGE = "Duplicate idempotency key";
    private static final String JASTIPER_ROLE = "JASTIPER";

    private final WalletService walletService;
    private final WalletRequestAccessPolicy walletRequestAccessPolicy;
    private final IdempotencyKeyGuard idempotencyKeyGuard;
    private final String midtransServerKey;

    public WalletController(
            WalletService walletService,
            WalletRequestAccessPolicy walletRequestAccessPolicy,
            IdempotencyKeyGuard idempotencyKeyGuard,
            @Value("${midtrans.server-key:}") String midtransServerKey
    ) {
        this.walletService = walletService;
        this.walletRequestAccessPolicy = walletRequestAccessPolicy;
        this.idempotencyKeyGuard = idempotencyKeyGuard;
        this.midtransServerKey = midtransServerKey == null ? "" : midtransServerKey;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(
            @PathVariable UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        validateReadAccess(authorization, userId);
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam UUID userId
    ) {
        validateMutationOwnerAccess(authorization, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walletService.createWallet(userId));
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody TopUpRequest request
    ) {
        validateMutationOwnerAccess(authorization, request.getUserId());
        if (walletRequestAccessPolicy.isForbiddenTopUpRole(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        return ResponseEntity.ok(walletService.topUp(request));
    }

    @PostMapping("/pay")
    public ResponseEntity<WalletResponse> pay(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody WalletMutationRequest request
    ) {
        validatePayAuthorization(authorization);
        validateIdempotencyKey(idempotencyKey);
        validateOwnerAccess(authorization, request.getUserId());
        return withIdempotencyKey(idempotencyKey, () -> ResponseEntity.ok(walletService.pay(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        )));
    }

    @PostMapping("/refund")
    public ResponseEntity<WalletResponse> refund(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody WalletMutationRequest request
    ) {
        validateMutationOwnerAccess(authorization, request.getUserId());
        return ResponseEntity.ok(walletService.refund(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        ));
    }

    @PostMapping("/payments/callback")
    public ResponseEntity<Map<String, String>> paymentCallback(
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signatureKey,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        requireHeader(signatureKey, SIGNATURE_HEADER);
        validateCallbackPayload(payload);
        validateCallbackSignature(payload, signatureKey);
        return ResponseEntity.ok(callbackAcceptedResponse());
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Role", required = false) String role,
            @Valid @RequestBody WalletMutationRequest request
    ) {
        validateMutationOwnerAccess(authorization, request.getUserId());
        validateWithdrawAccess(authorization, role, request.getUserId());

        return ResponseEntity.ok(walletService.withdraw(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        ));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID userId,
            @RequestParam(required = false) TransactionStatus status
    ) {
        validateReadAccess(authorization, userId);
        if (status != null) {
            return ResponseEntity.ok(walletService.getTransactionHistoryByStatus(userId, status));
        }
        return ResponseEntity.ok(walletService.getTransactionHistory(userId));
    }

    private boolean isMissingHeader(String headerValue) {
        return headerValue == null || headerValue.isBlank();
    }

    private boolean isJastiper(String role) {
        return JASTIPER_ROLE.equalsIgnoreCase(role);
    }

    private boolean hasValidJastiperJwt(String authorization) {
        return walletRequestAccessPolicy.isValidJastiperJwt(authorization);
    }

    private boolean hasPrivilegedWithdrawJwt(String authorization) {
        return walletRequestAccessPolicy.isValidAdminJwt(authorization)
                || hasValidJastiperJwt(authorization);
    }

    private boolean hasInvalidJwt(String authorization) {
        return walletRequestAccessPolicy.isInvalidJwtToken(authorization);
    }

    private void requireJwtBearerToken(String authorization) {
        if (!walletRequestAccessPolicy.isJwtBearerToken(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
    }

    private boolean isAuthorizedPayPrincipal(String authorization) {
        return walletRequestAccessPolicy.isAllowedPayRole(authorization);
    }

    private void validatePayAuthorization(String authorization) {
        if (walletRequestAccessPolicy.isDisallowedRoleForPay(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        if (isAuthorizedPayPrincipal(authorization)) {
            return;
        }
        if (shouldRejectAsUnauthorizedPayRequest(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        throw new ForbiddenException(FORBIDDEN_MESSAGE);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        requireHeader(idempotencyKey, IDEMPOTENCY_HEADER);
    }

    private void registerIdempotencyKeyOrThrow(String idempotencyKey) {
        if (!idempotencyKeyGuard.register(idempotencyKey)) {
            throw new ConflictException(DUPLICATE_IDEMPOTENCY_MESSAGE);
        }
    }

    private <T> T withIdempotencyKey(String idempotencyKey, Supplier<T> action) {
        registerIdempotencyKeyOrThrow(idempotencyKey);
        try {
            return action.get();
        } catch (RuntimeException ex) {
            idempotencyKeyGuard.release(idempotencyKey);
            throw ex;
        }
    }

    private void requireHeader(String value, String headerName) {
        if (isMissingHeader(value)) {
            throw new IllegalArgumentException("Missing required header: " + headerName);
        }
    }

    private boolean shouldRejectAsUnauthorizedPayRequest(String authorization) {
        return !walletRequestAccessPolicy.isJwtBearerToken(authorization)
                || !walletRequestAccessPolicy.isValidReadJwt(authorization);
    }

    private void validateCallbackPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Callback payload must not be empty");
        }
    }

    private Map<String, String> callbackAcceptedResponse() {
        return Map.of("message", CALLBACK_ACCEPTED_MESSAGE);
    }

    private void validateCallbackSignature(Map<String, Object> payload, String signatureKey) {
        String expectedSignature = buildExpectedSignature(payload);
        if (!expectedSignature.equals(signatureKey)) {
            throw new UnauthorizedException(INVALID_CALLBACK_SIGNATURE_MESSAGE);
        }
    }

    private String buildExpectedSignature(Map<String, Object> payload) {
        String orderId = extractRequiredPayloadValue(payload, "order_id");
        String statusCode = extractRequiredPayloadValue(payload, "status_code");
        String grossAmount = extractRequiredPayloadValue(payload, "gross_amount");
        String rawSignature = orderId + statusCode + grossAmount + midtransServerKey;
        return sha512Hex(rawSignature);
    }

    private String extractRequiredPayloadValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required callback field: " + key);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Missing required callback field: " + key);
        }
        return text;
    }

    private String sha512Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-512 algorithm is unavailable", ex);
        }
    }

    private void requireAuthorization(String authorization) {
        if (isMissingHeader(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
    }

    private void validateWithdrawAccess(String authorization, String role, UUID userId) {
        if (isPrivilegedWithdrawJwt(authorization)) {
            validateOwnerAccess(authorization, userId);
            return;
        }
        if (walletRequestAccessPolicy.isJwtBearerToken(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        if (isMissingOrNotJastiper(role)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
    }

    private boolean isPrivilegedWithdrawJwt(String authorization) {
        return walletRequestAccessPolicy.isJwtBearerToken(authorization)
                && hasPrivilegedWithdrawJwt(authorization);
    }

    private boolean isMissingOrNotJastiper(String role) {
        return isMissingHeader(role) || !isJastiper(role);
    }

    private void validateOwnerAccess(String authorization, UUID userId) {
        if (walletRequestAccessPolicy.isOwnerMismatchJwt(authorization, userId)
                || walletRequestAccessPolicy.isOwnerMismatchToken(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
    }

    private void validateMutationOwnerAccess(String authorization, UUID userId) {
        requireAuthorization(authorization);
        if (hasInvalidJwt(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        requireJwtBearerToken(authorization);
        if (!walletRequestAccessPolicy.isValidReadJwt(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        validateOwnerAccess(authorization, userId);
        validateMutationRoleAllowed(authorization);
    }

    private void validateSupportedRole(String authorization) {
        if (!walletRequestAccessPolicy.isAllowedWalletMutationRole(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
    }

    private void validateMutationRoleAllowed(String authorization) {
        validateSupportedRole(authorization);
    }

    private void validateReadRoleAllowed(String authorization) {
        validateSupportedRole(authorization);
    }

    private void validateReadAccess(String authorization, UUID userId) {
        requireAuthorization(authorization);
        if (hasInvalidJwt(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        requireJwtBearerToken(authorization);
        validateOwnerAccess(authorization, userId);
        if (!walletRequestAccessPolicy.isValidReadJwt(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        validateReadRoleAllowed(authorization);
    }
}
