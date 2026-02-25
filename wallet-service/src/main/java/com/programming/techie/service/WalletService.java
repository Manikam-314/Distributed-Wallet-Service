package com.programming.techie.service;

import org.springframework.stereotype.Service;
import com.programming.techie.dto.DepositRequest;
import com.programming.techie.entity.LedgerEntry;
import com.programming.techie.entity.WalletEntity;
import com.programming.techie.repository.LedgerRepository;
//import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.repository.WalletRepository;
import com.programming.techie.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    public WalletService(WalletRepository walletRepository,
                         LedgerRepository ledgerRepository) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
    }

    // ================= CREATE WALLET =================
    public void createWallet(Long userId) {
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        walletRepository.save(wallet);
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

        return ledgerRepository.calculateBalance(request.getWalletId());
    }

    // ⭐ NEW → DEBIT WALLET
    @Transactional
    public void debit(Long walletId, Double amount) {

        Double balance = ledgerRepository.calculateBalance(walletId);

        if (balance < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        LedgerEntry entry = new LedgerEntry();
        entry.setWalletId(walletId);
        entry.setAmount(-amount);
        entry.setType("DEBIT");

        ledgerRepository.save(entry);
    }

    // ⭐ NEW → CREDIT WALLET
    @Transactional
    public void credit(Long walletId, Double amount) {

        LedgerEntry entry = new LedgerEntry();
        entry.setWalletId(walletId);
        entry.setAmount(amount);
        entry.setType("CREDIT");

        ledgerRepository.save(entry);
    }

    // ================= BALANCE =================
    public Double getBalance(Long walletId) {
        return ledgerRepository.calculateBalance(walletId);
    }
}