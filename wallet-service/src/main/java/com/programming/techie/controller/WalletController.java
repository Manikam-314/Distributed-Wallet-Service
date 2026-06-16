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
    public ResponseEntity<BigDecimal> deposit(
            @RequestHeader(value = "loggedInUserId", required = false) String loggedInUserId,
            @RequestBody DepositRequest request) {
            
        if (loggedInUserId == null || loggedInUserId.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        
        WalletEntity wallet = walletService.getWalletById(request.getWalletId());
        if (!wallet.getUserId().equals(Long.parseLong(loggedInUserId))) {
            return ResponseEntity.status(403).build();
        }
        
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

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<WalletEntity> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletEntity> getById(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getWalletById(walletId));
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
                null, // manual external credit
                request.getWalletId(),
                BigDecimal.valueOf(request.getAmount())
        );
        return ResponseEntity.ok("Credited");
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> balance(@RequestParam Long walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }

    @GetMapping("/audit/rebuild")
    public ResponseEntity<BigDecimal> rebuild(@RequestParam Long walletId) {
        return ResponseEntity.ok(walletService.rebuildBalanceFromEvents(walletId));
    }
}
