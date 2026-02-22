package com.practice.practice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.practice.practice.dto.DepositRequest;
import com.practice.practice.dto.TransferRequest;
import com.practice.practice.model.WalletEntity;
import com.practice.practice.service.WalletService;

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

    // Transfer money
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestHeader("idempotency-key") String key,
            @RequestBody TransferRequest request) {
        walletService.transfer(request, key);
        return ResponseEntity.ok("Transfer successful");
    }

    // Check balance
    @GetMapping("/balance")
    public ResponseEntity<Double> balance(@RequestParam Long walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));

    }
}

// POST /users/register
// POST /wallet/deposit
// POST /wallet/transfer
// GET /wallet/balance
// GET /transactions