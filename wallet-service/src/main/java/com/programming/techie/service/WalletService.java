package com.programming.techie.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import com.programming.techie.dto.DepositRequest;
import com.programming.techie.dto.TransferRequest;
import com.programming.techie.enums.TransactionType;
import com.programming.techie.entity.IdempotencyKey;
import com.programming.techie.entity.LedgerEntry;
import com.programming.techie.entity.WalletEntity;
import com.programming.techie.repository.IdempotencyKeyRepository;
import com.programming.techie.repository.LedgerRepository;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.repository.WalletRepository;
import com.programming.techie.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    public WalletService(WalletRepository walletRepository,
                         IdempotencyKeyRepository idempotencyRepository,
                         TransactionRepository transactionRepository, LedgerRepository ledgerRepository) {
        this.walletRepository = walletRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
    }

    // ================= DEPOSIT =================
    @Transactional
    public Double deposit(DepositRequest request) {

        walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        LedgerEntry entry = new LedgerEntry();
        entry.setWalletId(request.getWalletId());
        entry.setAmount(request.getAmount());
        entry.setType("CREDIT");

        ledgerRepository.save(entry);

        // return updated balance
        return ledgerRepository.calculateBalance(request.getWalletId());
    }

    // ================= TRANSFER =================
    @Transactional
    public void transfer(TransferRequest request, String key) {

        if (idempotencyRepository.existsByKey(key)) {
            throw new RuntimeException("Duplicate request");
        }

        Double senderBalance = ledgerRepository.calculateBalance(request.getSenderWalletId());

        if (senderBalance < request.getAmount()) {
            throw new RuntimeException("Insufficient balance");
        }

        // sender debit
        LedgerEntry debit = new LedgerEntry();
        debit.setWalletId(request.getSenderWalletId());
        debit.setAmount(-request.getAmount());
        debit.setType("DEBIT");

        // receiver credit
        LedgerEntry credit = new LedgerEntry();
        credit.setWalletId(request.getReceiverWalletId());
        credit.setAmount(request.getAmount());
        credit.setType("CREDIT");

        ledgerRepository.save(debit);
        ledgerRepository.save(credit);

        IdempotencyKey record = new IdempotencyKey();
        record.setKey(key);
        record.setResponse("Transfer successful");

        idempotencyRepository.save(record);
    }

    // ================= BALANCE =================
    public Double getBalance(Long walletId) {
        return ledgerRepository.calculateBalance(walletId);
    }
}