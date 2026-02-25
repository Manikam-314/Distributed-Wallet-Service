package com.programming.techie.service;

import com.programming.techie.entity.Transaction;
import com.programming.techie.enums.TransactionType;
import com.programming.techie.repository.IdempotencyKeyRepository;
import com.programming.techie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.programming.techie.service.TransactionService;
import com.programming.techie.entity.IdempotencyKey;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final WebClient webClient;

    private final String WALLET_SERVICE_URL = "http://localhost:8081";

    // ⭐ TRANSFER LOGIC MOVED HERE
    public void transfer(Long senderWalletId, Long receiverWalletId,
                         Double amount, String key) {

        if (idempotencyRepository.existsByKey(key)) {
            throw new RuntimeException("Duplicate request");
        }

        // debit sender
        webClient.post()
                .uri(WALLET_SERVICE_URL +
                        "/wallet/debit?walletId=" + senderWalletId +
                        "&amount=" + amount)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // credit receiver
        webClient.post()
                .uri(WALLET_SERVICE_URL +
                        "/wallet/credit?walletId=" + receiverWalletId +
                        "&amount=" + amount)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // save transaction record
        Transaction tx = new Transaction();
        tx.setSenderWalletId(senderWalletId);
        tx.setReceiverWalletId(receiverWalletId);
        tx.setAmount(amount);
        tx.setType(TransactionType.TRANSFER);
        tx.setStatus("SUCCESS");

        transactionRepository.save(tx);

        IdempotencyKey record = new IdempotencyKey();
        record.setKey(key);
        record.setResponse("Transfer successful");

        idempotencyRepository.save(record);
    }
    // get transaction history of wallet
    public List<Transaction> getTransactions(Long walletId) {
        return transactionRepository
                .findBySenderWalletIdOrReceiverWalletId(walletId, walletId);
    }
}
