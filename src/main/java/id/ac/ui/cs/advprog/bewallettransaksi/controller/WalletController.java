package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
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
import id.ac.ui.cs.advprog.bewallettransaksi.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallet")
public class WalletController {
    private static final String UNAUTHORIZED_MESSAGE = "Autentikasi diperlukan!";
    private static final String FORBIDDEN_MESSAGE = "Akses ditolak!";
    private static final String JASTIPER_ROLE = "JASTIPER";

    private final WalletService walletService;
    private final WalletRequestAccessPolicy walletRequestAccessPolicy;
    private final String acceptedBearerToken;

    public WalletController(
            WalletService walletService,
            WalletRequestAccessPolicy walletRequestAccessPolicy,
            @Value("${wallet.auth.accepted-bearer-token:Bearer test-token}") String acceptedBearerToken
    ) {
        this.walletService = walletService;
        this.walletRequestAccessPolicy = walletRequestAccessPolicy;
        this.acceptedBearerToken = acceptedBearerToken;
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
            @RequestHeader(value = "X-Role", required = false) String role,
            @Valid @RequestBody TopUpRequest request
    ) {
        validateMutationOwnerAccess(authorization, request.getUserId());
        if (walletRequestAccessPolicy.isForbiddenTopUpRole(authorization, role)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        return ResponseEntity.ok(walletService.topUp(request));
    }

    @PostMapping("/pay")
    public ResponseEntity<WalletResponse> pay(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody WalletMutationRequest request
    ) {
        validatePayAuthorization(authorization);
        validateOwnerAccess(authorization, request.getUserId());
        return ResponseEntity.ok(walletService.pay(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        ));
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

    private boolean isAuthorizedForCurrentContract(String authorization) {
        return acceptedBearerToken.equals(authorization);
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
        return walletRequestAccessPolicy.isAllowedPayRole(authorization)
                || isAuthorizedForCurrentContract(authorization);
    }

    private void validatePayAuthorization(String authorization) {
        if (walletRequestAccessPolicy.isDisallowedRoleForPay(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        if (isAuthorizedPayPrincipal(authorization)) {
            return;
        }
        if (!walletRequestAccessPolicy.isJwtBearerToken(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        if (walletRequestAccessPolicy.isValidReadJwt(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
    }

    private void requireAuthorization(String authorization) {
        if (isMissingHeader(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
    }

    private void validateWithdrawAccess(String authorization, String role, UUID userId) {
        if (walletRequestAccessPolicy.isJwtBearerToken(authorization)) {
            if (!hasPrivilegedWithdrawJwt(authorization)) {
                throw new ForbiddenException(FORBIDDEN_MESSAGE);
            }
            validateOwnerAccess(authorization, userId);
            return;
        }
        if (hasPrivilegedWithdrawJwt(authorization)) {
            validateOwnerAccess(authorization, userId);
            return;
        }
        if (isMissingOrNotJastiper(role)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
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
