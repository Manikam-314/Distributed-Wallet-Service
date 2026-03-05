package com.programming.techie.controller;
import com.programming.techie.dto.CreateWalletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.programming.techie.dto.DepositRequest;
import com.programming.techie.dto.TransferRequest;
import com.programming.techie.entity.WalletEntity;
import com.programming.techie.service.WalletService;

import java.math.BigDecimal;
@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<BigDecimal> deposit(@RequestBody DepositRequest request) {
        return ResponseEntity.ok(
                walletService.deposit(
                        request.getWalletId(),
                        BigDecimal.valueOf(request.getAmount())
                )
        );
    }

    @PostMapping("/create")
    public ResponseEntity<String> createWallet(@RequestBody CreateWalletRequest request) {
        walletService.createWallet(request.getUserId());
        return ResponseEntity.ok("Wallet created");
    }

    @PostMapping("/debit")
    public ResponseEntity<String> debit(@RequestBody DepositRequest request) {
        walletService.debit(
                request.getWalletId(),
                BigDecimal.valueOf(request.getAmount())
        );
        return ResponseEntity.ok("Debited");
    }

    @PostMapping("/credit")
    public ResponseEntity<String> credit(@RequestBody DepositRequest request) {
        walletService.credit(
                request.getWalletId(),
                BigDecimal.valueOf(request.getAmount())
        );
        return ResponseEntity.ok("Credited");
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> balance(@RequestParam Long walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }
}