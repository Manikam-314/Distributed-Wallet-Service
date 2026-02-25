package com.programming.techie.controller;

import com.programming.techie.dto.TransferRequest;
import com.programming.techie.entity.Transaction;
import com.programming.techie.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // ================= TRANSFER =================
    // POST /transactions/transfer
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(
            @RequestHeader("idempotency-key") String key,
            @RequestBody TransferRequest request
    ) {
        transactionService.transfer(
                request.getSenderWalletId(),
                request.getReceiverWalletId(),
                request.getAmount(),
                key
        );        return ResponseEntity.ok("Transfer successful");
    }

    // ================= TRANSACTION HISTORY =================
    // GET /transactions?walletId=1
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam Long walletId
    ){
        return ResponseEntity.ok(transactionService.getTransactions(walletId));
    }
}