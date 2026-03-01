package com.programming.techie.service;

import com.programming.techie.entity.Transaction;
import com.programming.techie.enums.TransactionType;
import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.repository.IdempotencyKeyRepository;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.entity.IdempotencyKey;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyRepository;

    // ⭐ Kafka producer
    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    private static final String TRANSACTION_TOPIC = "transaction-created";

    /**
     * Transfer money using event-driven architecture.
     * No REST calls to wallet service.
     */
    public void transfer(Long senderWalletId,
                         Long receiverWalletId,
                         Double amount,
                         String idempotencyKey) {

        // ================= IDENTITY CHECK =================
        if (idempotencyRepository.existsByKey(idempotencyKey)) {
            throw new RuntimeException("Duplicate request");
        }

        // ================= SAVE TRANSACTION =================
        Transaction tx = new Transaction();
        tx.setSenderWalletId(senderWalletId);
        tx.setReceiverWalletId(receiverWalletId);
        tx.setAmount(amount);
        tx.setType(TransactionType.TRANSFER);
        tx.setStatus("PENDING"); // important for distributed systems

        Transaction savedTx = transactionRepository.save(tx);

        // ================= CREATE EVENT =================
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                savedTx.getId(),
                senderWalletId,
                receiverWalletId,
                amount,
                Instant.now(),
                idempotencyKey
        );

        // ================= PUBLISH EVENT =================
        kafkaTemplate.send(
                TRANSACTION_TOPIC,
                savedTx.getId().toString(), // key for ordering
                event
        );

        // ================= STORE IDEMPOTENCY =================
        IdempotencyKey record = new IdempotencyKey();
        record.setKey(idempotencyKey);
        record.setResponse("Transaction initiated");

        idempotencyRepository.save(record);
    }

    // ================= TRANSACTION HISTORY =================
    public List<Transaction> getTransactions(Long walletId) {
        return transactionRepository
                .findBySenderWalletIdOrReceiverWalletId(walletId, walletId);
    }
}
