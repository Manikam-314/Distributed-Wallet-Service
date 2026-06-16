package com.programming.techie.consumer;

import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.entity.ProcessedEvent;
import com.programming.techie.monitoring.KafkaMetricsService;
import com.programming.techie.repository.ProcessedEventRepository;
import com.programming.techie.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TransactionConsumer {

    private final WalletService walletService;
    private final ProcessedEventRepository processedRepo;
    private final KafkaMetricsService metricsService;

    @KafkaListener(topics = "transaction-created", groupId = "wallet-group")
    @Transactional
    public void consume(TransactionCreatedEvent event) {

        String txnId = String.valueOf(event.getTransactionId());

        System.out.println("EVENT RECEIVED: " + txnId);

        metricsService.incrementTotal();

        if (processedRepo.existsById(txnId)) {
            System.out.println("Duplicate event ignored (Inbox)");
            return;
        }

        try {

            walletService.processTransaction(
                    event.getTransactionId(),
                    event.getFromWalletId(),
                    event.getAmount()
            );

            processedRepo.save(new ProcessedEvent(txnId));

            metricsService.incrementSuccess();
            metricsService.printMetrics();

        } catch (Exception e) {

            metricsService.incrementFailure();
            metricsService.printMetrics();

            throw e;
        }
    }
}
