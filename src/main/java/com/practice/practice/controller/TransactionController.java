// package com.practice.practice.controller;

// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// import com.practice.practice.service.TransactionService;

// @RestController
// @RequestMapping("/transaction")
// public class TransactionController {

//     private final TransactionService transactionService;

//     public TransactionController(TransactionService transactionService) {
//         this.transactionService = transactionService;
//     }
//      // GET /transactions?walletId=1
//     @GetMapping
//     public List<Transaction> getTransactions(@RequestParam Long walletId){
//         return transactionService.getTransactions(walletId);
//     }}
    
    package com.practice.practice.controller;

import com.practice.practice.model.Transaction;
import com.practice.practice.service.TransactionService;
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
