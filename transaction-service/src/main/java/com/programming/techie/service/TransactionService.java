package com.programming.techie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.client.WalletClient;
import com.programming.techie.entity.Transaction;
import com.programming.techie.enums.TransactionType;
import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.repository.IdempotencyRecordRepository;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.inbox.entity.OutboxEvent;
import com.programming.techie.inbox.repository.OutboxRepository;
import com.programming.techie.saga.entity.TransferSaga;
import com.programming.techie.saga.status.SagaStatus;
import com.programming.techie.saga.repository.TransferSagaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final TransferSagaRepository sagaRepository;
    private final ObjectMapper objectMapper;
    private final WalletClient walletClient;
    private final com.programming.techie.client.AuthClient authClient;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TRANSACTION_TOPIC = "transaction-created";

    @Transactional
    public void transfer(Long senderWalletId,
            Long receiverWalletId,
            BigDecimal amount,
            String idempotencyKey,
            Long loggedInUserId) {

        // 1️⃣ IDEMPOTENCY CHECK
        if (idempotencyKey != null && idempotencyRepository.existsById(idempotencyKey)) {
            throw new com.programming.techie.exception.DuplicateTransactionException("Duplicate request structure for idempotency key.");
        }

        // 2️⃣ VALIDATE AMOUNT
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid transfer amount");
        }

        // 2.5️⃣ PREVENT SELF-TRANSFER
        if (senderWalletId.equals(receiverWalletId)) {
            throw new RuntimeException("Cannot transfer money to yourself");
        }
        
        // 2.8️⃣ VALIDATE OWNERSHIP (Fix IDOR)
        com.programming.techie.dto.WalletDTO senderWallet = walletClient.getWallet(senderWalletId);
        if (senderWallet == null || !senderWallet.getUserId().equals(loggedInUserId)) {
            throw new RuntimeException("Unauthorized: You do not own the sender wallet.");
        }

        // 3️⃣ CHECK BALANCE VIA REDIS CACHE (Task 13: Loose Coupling)
        BigDecimal balance = getBalanceFromCache(senderWalletId);

        if (balance == null) {
            // Fallback to sync call if cache is missing (one-time penalty to populate)
            balance = walletClient.getBalance(senderWalletId);
        }

        if (balance.compareTo(amount) < 0) {
            throw new com.programming.techie.exception.InsufficientFundsException("Insufficient balance");
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
                idempotencyKey);

        // 6.5️⃣ SEND NOTIFICATION (Asynchronous side-effect)
        try {
            // Get Sender info for the message
            com.programming.techie.dto.WalletDTO receiverWallet = walletClient.getWallet(receiverWalletId);
            if (receiverWallet != null) {
                com.programming.techie.dto.UserDTO recipientUser = authClient.getUser(receiverWallet.getUserId());
                com.programming.techie.dto.UserDTO senderUser = authClient.getUser(loggedInUserId);
                
                if (recipientUser != null) {
                    com.programming.techie.events.NotificationEvent notif = com.programming.techie.events.NotificationEvent.builder()
                            .mobileNumber(recipientUser.getMobileNumber())
                            .email(recipientUser.getEmail())
                            .message(String.format("Money Received! %s has sent you ₹%.2f", 
                                    senderUser != null ? senderUser.getName() : "A user",
                                    amount))
                            .deliveryChannel("BOTH")
                            .build();
                    
                    kafkaTemplate.send("notificationTopic", notif);
                }
            }
        } catch (Exception e) {
            // Log but don't fail transaction for a notification side-effect
            log.error("Notification failed for transaction {}: {}", savedTx.getId(), e.getMessage());
        }

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
                .aggregateId(senderWalletId.toString()) // Hot Wallet Strategy: Partition by Wallet ID
                .payload(payload)
                .topic(TRANSACTION_TOPIC)
                .published(false)
                .createdAt(Instant.now())
                .build();

        outboxRepository.save(outboxEvent);

        // 9️⃣ STORE IDEMPOTENCY
        com.programming.techie.entity.IdempotencyRecord record = new com.programming.techie.entity.IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setServiceName("transaction-service");
        record.setProcessedAt(Instant.now());

        idempotencyRepository.save(record);
    }

    public List<Transaction> getTransactions(Long walletId) {
        return transactionRepository
                .findBySenderWalletIdOrReceiverWalletId(walletId, walletId);
    }

    public BigDecimal getBalance(Long walletId) {
        BigDecimal balance = getBalanceFromCache(walletId);
        return balance != null ? balance : walletClient.getBalance(walletId);
    }

    private java.math.BigDecimal getBalanceFromCache(Long walletId) {
        try {
            String key = "wallet:balance::" + walletId; // Spring cache default with @Cacheable(value="wallet:balance")
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof java.math.BigDecimal) return (java.math.BigDecimal) value;
            if (value instanceof Double) return java.math.BigDecimal.valueOf((Double) value);
            if (value instanceof Integer) return java.math.BigDecimal.valueOf((Integer) value);
            if (value instanceof String) return new java.math.BigDecimal((String) value);
        } catch (Exception e) {
            // Log but don't fail, we have fallback
        }
        return null;
    }
}
