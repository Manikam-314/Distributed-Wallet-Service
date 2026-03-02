package com.programming.techie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.entity.Transaction;
import com.programming.techie.enums.TransactionType;
import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.repository.IdempotencyKeyRepository;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.entity.IdempotencyKey;
import com.programming.techie.outbox.entity.OutboxEvent;
import com.programming.techie.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final String TRANSACTION_TOPIC = "transaction-created";

    @Transactional
    public void transfer(Long senderWalletId,
                         Long receiverWalletId,
                         Double amount,
                         String idempotencyKey) {

        // ===== 1️⃣ IDEMPOTENCY CHECK =====
        if (idempotencyRepository.existsByKey(idempotencyKey)) {
            throw new RuntimeException("Duplicate request");
        }

        // ===== 2️⃣ SAVE TRANSACTION =====
        Transaction tx = new Transaction();
        tx.setSenderWalletId(senderWalletId);
        tx.setReceiverWalletId(receiverWalletId);
        tx.setAmount(amount);
        tx.setType(TransactionType.TRANSFER);
        tx.setStatus("PENDING");

        Transaction savedTx = transactionRepository.save(tx);

        // ===== 3️⃣ CREATE EVENT OBJECT =====
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                savedTx.getId(),
                senderWalletId,
                receiverWalletId,
                amount,
                Instant.now(),
                idempotencyKey
        );

        // ===== 4️⃣ SAVE EVENT IN OUTBOX =====
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("TRANSACTION")
                .aggregateId(savedTx.getId().toString())
                .payload(payload)
                .topic(TRANSACTION_TOPIC)
                .published(false)
                .createdAt(Instant.now())
                .build();

        outboxRepository.save(outboxEvent);

        // ===== 5️⃣ STORE IDEMPOTENCY =====
        IdempotencyKey record = new IdempotencyKey();
        record.setKey(idempotencyKey);
        record.setResponse("Transaction initiated");

        idempotencyRepository.save(record);
    }

    public List<Transaction> getTransactions(Long walletId) {
        return transactionRepository
                .findBySenderWalletIdOrReceiverWalletId(walletId, walletId);
    }
}