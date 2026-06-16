package com.programming.techie.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.events.WalletCompensationEvent;
import com.programming.techie.entity.Transaction;
import com.programming.techie.events.TransactionProcessedEvent;
import com.programming.techie.events.WalletCreditEvent;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.saga.entity.TransferSaga;
import com.programming.techie.saga.repository.TransferSagaRepository;
import com.programming.techie.saga.status.SagaStatus;
import com.programming.techie.inbox.entity.OutboxEvent;
import com.programming.techie.inbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TransactionProcessedConsumer {

    private final TransactionRepository transactionRepository;
    private final TransferSagaRepository sagaRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "transaction-processed",
            groupId = "transaction-group"
    )
    @Transactional
    public void consume(TransactionProcessedEvent event) {

        System.out.println("SAGA CONSUMER HIT → " + event.getTransactionId());

        Transaction tx = transactionRepository
                .findById(event.getTransactionId())
                .orElseThrow();

        TransferSaga saga = sagaRepository
                .findByTransactionId(event.getTransactionId())
                .orElseThrow();

        if ("SUCCESS".equals(event.getStatus())) {

            saga.setStatus(SagaStatus.COMPLETED);
            tx.setStatus("SUCCESS");

            // ⭐ CREDIT RECEIVER WALLET (OUTBOX PATTERN / partitioned by receiver wallet)
            WalletCreditEvent creditEvent = new WalletCreditEvent(
                    tx.getId(),
                    tx.getReceiverWalletId(),
                    tx.getAmount()
            );
            saveOutbox("wallet-credit", tx.getReceiverWalletId().toString(), creditEvent);
        } else {

            tx.setStatus("FAILED");
            saga.setStatus(SagaStatus.COMPENSATING);

            WalletCompensationEvent compensationEvent =
                    new WalletCompensationEvent(
                            tx.getId(),
                            tx.getSenderWalletId(),
                            tx.getAmount()
            );

            saveOutbox("wallet-compensation-requested", tx.getSenderWalletId().toString(), compensationEvent);
        }

        saga.setUpdatedAt(Instant.now());
        sagaRepository.save(saga);
        transactionRepository.save(tx);
    }
    
    private void saveOutbox(String topic, String aggregateId, Object payloadObj) {
        try {
            String payload = objectMapper.writeValueAsString(payloadObj);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("TRANSACTION")
                    .aggregateId(aggregateId)
                    .payload(payload)
                    .topic(topic)
                    .published(false)
                    .createdAt(Instant.now())
                    .build();
            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
