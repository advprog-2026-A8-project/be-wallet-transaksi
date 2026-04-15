package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.util.UUID;
import java.util.List;

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
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID userId) {
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@RequestParam UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walletService.createWallet(userId));
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(request));
    }

    @PostMapping("/pay")
    public ResponseEntity<WalletResponse> pay(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody WalletMutationRequest request
    ) {
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(walletService.pay(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        ));
    }

    @PostMapping("/refund")
    public ResponseEntity<WalletResponse> refund(@Valid @RequestBody WalletMutationRequest request) {
        return ResponseEntity.ok(walletService.refund(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        ));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(@Valid @RequestBody WalletMutationRequest request) {
        return ResponseEntity.ok(walletService.withdraw(
                request.getUserId(),
                request.getAmount(),
                request.getDescription()
        ));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable UUID userId,
            @RequestParam(required = false) TransactionStatus status
    ) {
        if (status != null) {
            return ResponseEntity.ok(walletService.getTransactionHistoryByStatus(userId, status));
        }
        return ResponseEntity.ok(walletService.getTransactionHistory(userId));
    }
}
