package com.programming.techie.controller;

import com.programming.techie.entity.Transaction;
import com.programming.techie.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService){
        this.transactionService = transactionService;
    }

    // GET /transactions?walletId=1
    @GetMapping
    public List<Transaction> getTransactions(@RequestParam Long walletId){
        return transactionService.getTransactions(walletId);
    }
}
