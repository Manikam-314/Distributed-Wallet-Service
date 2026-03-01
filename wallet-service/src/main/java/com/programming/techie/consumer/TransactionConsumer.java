package com.programming.techie.consumer;

import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.entity.ProcessedEvent;
import com.programming.techie.repository.ProcessedEventRepository;
import com.programming.techie.service.WalletService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

    private final WalletService walletService;
    private final ProcessedEventRepository processedRepo;

    public TransactionConsumer(WalletService walletService,
                               ProcessedEventRepository processedRepo) {
        this.walletService = walletService;
        this.processedRepo = processedRepo;
    }

    @KafkaListener(topics = "transaction-created")
    public void consume(TransactionCreatedEvent event) {

        System.out.println("EVENT RECEIVED: " + event.getTransactionId());

        String txnId = String.valueOf(event.getTransactionId());

        if (processedRepo.existsById(txnId)) {
            System.out.println("Duplicate event ignored");
            return;
        }

        try {

            walletService.processTransaction(
                    event.getTransactionId(),
                    event.getFromWalletId(),
                    event.getAmount()
            );

            processedRepo.save(new ProcessedEvent(txnId));

        } catch (Exception e) {
            throw e; // retry + DLQ
        }
    }
}

