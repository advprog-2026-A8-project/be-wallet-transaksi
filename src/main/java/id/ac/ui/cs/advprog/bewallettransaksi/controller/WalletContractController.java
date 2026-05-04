package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.CheckBalanceResult;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.DeductBalanceRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.OrderWalletContractService;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.RefundBalanceRequest;
import id.ac.ui.cs.advprog.bewallettransaksi.service.contract.WalletMutationResult;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.ForbiddenException;
import id.ac.ui.cs.advprog.bewallettransaksi.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts/wallet")
public class WalletContractController {
    private static final String UNAUTHORIZED_MESSAGE = "Autentikasi diperlukan!";
    private static final String FORBIDDEN_MESSAGE = "Akses ditolak!";

    private final OrderWalletContractService contractService;
    private final WalletRequestAccessPolicy accessPolicy;

    public WalletContractController(
            OrderWalletContractService contractService,
            WalletRequestAccessPolicy accessPolicy
    ) {
        this.contractService = contractService;
        this.accessPolicy = accessPolicy;
    }

    @PostMapping("/check-balance")
    public ResponseEntity<WalletMutationResult> checkBalance(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CheckBalanceRequest request
    ) {
        validateContractAccess(authorization);
        CheckBalanceResult result = contractService.checkBalance(request);
        return ResponseEntity.ok(new WalletMutationResult(result.sufficient(), result.currentBalance(), null));
    }

    @PostMapping("/deduct")
    public ResponseEntity<WalletMutationResult> deduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody DeductBalanceRequest request
    ) {
        validateContractAccess(authorization);
        return ResponseEntity.ok(contractService.deductBalance(request));
    }

    @PostMapping("/refund")
    public ResponseEntity<WalletMutationResult> refund(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody RefundBalanceRequest request
    ) {
        validateContractAccess(authorization);
        return ResponseEntity.ok(contractService.refundBalance(request));
    }

    private void validateContractAccess(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        if (!accessPolicy.isJwtBearerToken(authorization) || !accessPolicy.isValidReadJwt(authorization)) {
            throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
        }
        if (!accessPolicy.isAllowedWalletMutationRole(authorization)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
    }
}
