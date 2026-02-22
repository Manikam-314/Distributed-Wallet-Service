package com.practice.practice.service;

import com.practice.practice.model.Transaction;
import com.practice.practice.model.WalletEntity;
import com.practice.practice.repository.IdempotencyKeyRepository;
import com.practice.practice.repository.TransactionRepository;
import com.practice.practice.repository.WalletRepository;
import org.springframework.stereotype.Service;
import com.practice.practice.repository.IdempotencyKeyRepository;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    public TransactionService(TransactionRepository transactionRepository,    IdempotencyKeyRepository idempotencyRepository,

                              WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.idempotencyRepository=idempotencyRepository;
    }

    // get transaction history of wallet
    public List<Transaction> getTransactions(Long walletId){

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return transactionRepository.findBySenderWalletOrReceiverWallet(wallet, wallet);
    }
}
