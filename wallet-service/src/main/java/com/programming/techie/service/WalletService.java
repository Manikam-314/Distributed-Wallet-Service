package com.programming.techie.service;
import com.programming.techie.events.TransactionProcessedEvent;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WalletService(WalletRepository walletRepository,
                         LedgerRepository ledgerRepository,
                         KafkaTemplate<String, Object> kafkaTemplate) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.kafkaTemplate = kafkaTemplate;
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

    @Transactional
    public void processTransaction(Long transactionId, Long walletId, Double amount) {

        try {
            debit(walletId, amount);

            System.out.println("SENDING SUCCESS EVENT → " + transactionId);

            kafkaTemplate.send(
                    "transaction-processed",
                    transactionId.toString(),
                    new TransactionProcessedEvent(transactionId, "SUCCESS", null)
            );

        } catch (Exception e) {

            System.out.println("SENDING FAILED EVENT → " + transactionId);

            kafkaTemplate.send(
                    "transaction-processed",
                    transactionId.toString(),
                    new TransactionProcessedEvent(transactionId, "FAILED", e.getMessage())
            );

            throw e;
        }
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
