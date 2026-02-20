package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.model.Wallet;
import id.ac.ui.cs.advprog.bewallettransaksi.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}")
    public Wallet getWallet(@PathVariable UUID userId) {
        return walletService.getWallet(userId);
    }

    @PostMapping("/topup")
    public Wallet topUp(
            @RequestParam UUID userId,
            @RequestParam BigDecimal amount
    ) {
        return walletService.topUp(userId, amount);
    }
}