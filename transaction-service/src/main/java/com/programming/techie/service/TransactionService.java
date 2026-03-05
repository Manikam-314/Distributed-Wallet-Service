package com.programming.techie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.client.WalletClient;
import com.programming.techie.dto.TransferRequest;
import com.programming.techie.entity.Transaction;
import com.programming.techie.enums.TransactionType;
import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.repository.IdempotencyKeyRepository;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.entity.IdempotencyKey;
import com.programming.techie.inbox.entity.OutboxEvent;
import com.programming.techie.inbox.repository.OutboxRepository;
import com.programming.techie.saga.entity.TransferSaga;
import com.programming.techie.saga.status.SagaStatus;
import com.programming.techie.saga.repository.TransferSagaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final TransferSagaRepository sagaRepository;
    private final ObjectMapper objectMapper;
    private final WalletClient walletClient;

    private static final String TRANSACTION_TOPIC = "transaction-created";

    @Transactional
    public void transfer(Long senderWalletId,
                         Long receiverWalletId,
                         BigDecimal amount,
                         String idempotencyKey) {

        // 1️⃣ IDEMPOTENCY CHECK
        if (idempotencyRepository.existsByKey(idempotencyKey)) {
            throw new RuntimeException("Duplicate request");
        }

        // 2️⃣ VALIDATE AMOUNT
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid transfer amount");
        }

        // 3️⃣ CHECK BALANCE BEFORE EVENT
        BigDecimal balance = walletClient.getBalance(senderWalletId);

        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // 4️⃣ SAVE TRANSACTION
        Transaction tx = new Transaction();
        tx.setSenderWalletId(senderWalletId);
        tx.setReceiverWalletId(receiverWalletId);
        tx.setAmount(amount);
        tx.setType(TransactionType.TRANSFER);
        tx.setStatus("PENDING");

        Transaction savedTx = transactionRepository.save(tx);

        // 5️⃣ CREATE SAGA
        TransferSaga saga = TransferSaga.builder()
                .transactionId(savedTx.getId())
                .status(SagaStatus.INITIATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sagaRepository.save(saga);

        // 6️⃣ CREATE EVENT
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                savedTx.getId(),
                senderWalletId,
                receiverWalletId,
                amount,
                Instant.now(),
                idempotencyKey
        );

        // 7️⃣ SERIALIZE EVENT
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Event serialization failed", e);
        }

        // 8️⃣ SAVE OUTBOX EVENT
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("TRANSACTION")
                .aggregateId(savedTx.getId().toString())
                .payload(payload)
                .topic(TRANSACTION_TOPIC)
                .published(false)
                .createdAt(Instant.now())
                .build();

        outboxRepository.save(outboxEvent);

        // 9️⃣ STORE IDEMPOTENCY
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