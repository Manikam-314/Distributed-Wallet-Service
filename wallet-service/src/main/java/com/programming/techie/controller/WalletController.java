package com.programming.techie.controller;
import com.programming.techie.dto.CreateWalletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.programming.techie.dto.DepositRequest;
import com.programming.techie.dto.TransferRequest;
import com.programming.techie.entity.WalletEntity;
import com.programming.techie.service.WalletService;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // Deposit money
    @PostMapping("/deposit")
    public ResponseEntity<Double> deposit(@RequestBody DepositRequest request) {
        return ResponseEntity.ok(walletService.deposit(request));
    }
    @PostMapping("/create")
    public ResponseEntity<String> createWallet(@RequestBody CreateWalletRequest request) {
        walletService.createWallet(request.getUserId());
        return ResponseEntity.ok("Wallet created");
    }
    @PostMapping("/debit")
    public ResponseEntity<String> debit(@RequestParam Long walletId,
                                        @RequestParam Double amount) {
        walletService.debit(walletId, amount);
        return ResponseEntity.ok("Debited");
    }

    @PostMapping("/credit")
    public ResponseEntity<String> credit(@RequestParam Long walletId,
                                         @RequestParam Double amount) {
        walletService.credit(walletId, amount);
        return ResponseEntity.ok("Credited");
    }

    // Check balance
    @GetMapping("/balance")
    public ResponseEntity<Double> balance(@RequestParam Long walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));

    }
}
