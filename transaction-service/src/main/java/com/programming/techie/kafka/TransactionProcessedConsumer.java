package com.programming.techie.kafka;

import com.programming.techie.entity.Transaction;
import com.programming.techie.events.TransactionProcessedEvent;
import com.programming.techie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionProcessedConsumer {

    private final TransactionRepository transactionRepository;

    @KafkaListener(
            topics = "transaction-processed",
            groupId = "transaction-group"
    )    public void consume(TransactionProcessedEvent event) {

        System.out.println("RESULT RECEIVED: " + event.getTransactionId());

        Transaction tx = transactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        tx.setStatus(event.getStatus());

        transactionRepository.save(tx);
    }
}
