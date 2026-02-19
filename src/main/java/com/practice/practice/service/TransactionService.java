package com.practice.practice.service;

import com.practice.practice.model.Transaction;
import com.practice.practice.model.WalletEntity;
import com.practice.practice.repository.TransactionRepository;
import com.practice.practice.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    // get transaction history of wallet
    public List<Transaction> getTransactions(Long walletId){

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return transactionRepository.findBySenderWalletOrReceiverWallet(wallet, wallet);
    }
}
