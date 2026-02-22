package com.practice.practice.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import com.practice.practice.dto.DepositRequest;
import com.practice.practice.dto.TransferRequest;
import com.practice.practice.enumss.TransactionType;
import com.practice.practice.model.IdempotencyKey;
import com.practice.practice.model.WalletEntity;
import com.practice.practice.repository.IdempotencyKeyRepository;
import com.practice.practice.repository.TransactionRepository;
import com.practice.practice.repository.WalletRepository;
import com.practice.practice.model.Transaction;
import jakarta.transaction.Transactional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository,
            IdempotencyKeyRepository idempotencyRepository,
            TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletEntity deposit(DepositRequest depositRequest) {
        // 1. Find existing wallet
        WalletEntity wallet = walletRepository.findById(depositRequest.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // 2. Update balance
        wallet.setBalance(wallet.getBalance() + depositRequest.getAmount());

        // 3. Save wallet
        return walletRepository.save(wallet);

        // save in DB
    }

    // ================= TRANSFER =================
    @Transactional
    public void transfer(TransferRequest request, String key) {

        try {
            if (idempotencyRepository.existsByKey(key)) {
                throw new RuntimeException("Duplicate request");
            }
            WalletEntity sender = walletRepository.findById(request.getSenderWalletId())
                    .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

            WalletEntity receiver = walletRepository.findById(request.getReceiverWalletId())
                    .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

            if (sender.getBalance() < request.getAmount()) {
                throw new RuntimeException("Insufficient balance");
            }

            // ✅ update balances
            sender.setBalance(sender.getBalance() - request.getAmount());
            receiver.setBalance(receiver.getBalance() + request.getAmount());
            walletRepository.save(sender);
            walletRepository.save(receiver);

            // DEBIT entry
            Transaction debit = new Transaction();
            debit.setSenderWallet(sender);
            debit.setAmount(request.getAmount());
            debit.setType(TransactionType.DEBIT);
            System.out.println("TRANSFER STARTED");
            System.out.println("Saving debit transaction");
            System.out.println("Saving credit transaction");
            // CREDIT entry
            Transaction credit = new Transaction();
            credit.setReceiverWallet(receiver);
            credit.setAmount(request.getAmount());
            credit.setType(TransactionType.CREDIT);

            transactionRepository.save(debit);
            transactionRepository.save(credit);

            // 2. store idempotency key after success
            IdempotencyKey record = new IdempotencyKey();
            record.setKey(key);
            record.setResponse("Transfer successful");

            idempotencyRepository.save(record);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Transaction failed — wallet updated by another request. Try again.");
        }
        System.out.println("TRANSFER COMPLETED");
    }

    // ================= CHECK BALANCE =================
    public Double getBalance(Long walletId) {

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return wallet.getBalance();
    }
}