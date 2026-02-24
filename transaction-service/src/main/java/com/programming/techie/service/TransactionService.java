package com.programming.techie.service;

import com.programming.techie.entity.Transaction;
import com.programming.techie.entity.WalletEntity;
import com.programming.techie.repository.IdempotencyKeyRepository;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final IdempotencyKeyRepository idempotencyRepository;

    // get transaction history of wallet
    public List<Transaction> getTransactions(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return transactionRepository.findBySenderWalletOrReceiverWallet(wallet, wallet);
    }
}
