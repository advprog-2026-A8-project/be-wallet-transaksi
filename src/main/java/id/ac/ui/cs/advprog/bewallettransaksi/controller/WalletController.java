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
        requireAuthorization(authorization);
        if (walletRequestAccessPolicy.isInvalidJwtToken(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        if (!walletRequestAccessPolicy.isValidReadJwt(authorization)
                && !walletRequestAccessPolicy.isOwnerMismatchToken(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        if (walletRequestAccessPolicy.isOwnerMismatchToken(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam UUID userId
    ) {
        requireAuthorization(authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walletService.createWallet(userId));
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Role", required = false) String role,
            @Valid @RequestBody TopUpRequest request
    ) {
        requireAuthorization(authorization);
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
        if (walletRequestAccessPolicy.isDisallowedRoleForPay(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        if (!walletRequestAccessPolicy.isValidTitiperJwt(authorization)
                && !isAuthorizedForCurrentContract(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }

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
        requireAuthorization(authorization);
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
        requireAuthorization(authorization);
        validateWithdrawAccess(authorization, role);

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
        requireAuthorization(authorization);
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

    private void requireAuthorization(String authorization) {
        if (isMissingHeader(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
    }

    private void validateWithdrawAccess(String authorization, String role) {
        if (walletRequestAccessPolicy.isValidJastiperJwt(authorization)) {
            return;
        }
        if (isMissingHeader(role)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        if (!isJastiper(role)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
    }
}
