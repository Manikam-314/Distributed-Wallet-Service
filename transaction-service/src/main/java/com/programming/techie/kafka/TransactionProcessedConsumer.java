package com.programming.techie.kafka;

import com.programming.techie.events.WalletCompensationEvent;
import com.programming.techie.entity.Transaction;
import com.programming.techie.events.TransactionProcessedEvent;
import com.programming.techie.events.WalletCreditEvent;
import com.programming.techie.repository.TransactionRepository;
import com.programming.techie.saga.entity.TransferSaga;
import com.programming.techie.saga.repository.TransferSagaRepository;
import com.programming.techie.saga.status.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TransactionProcessedConsumer {

    private final TransactionRepository transactionRepository;
    private final TransferSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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

            // ⭐ CREDIT RECEIVER WALLET
            kafkaTemplate.send(
                    "wallet-credit",
                    new WalletCreditEvent(
                            tx.getReceiverWalletId(),
                            tx.getAmount()
                    )
            );
        } else {

            tx.setStatus("FAILED");

            saga.setStatus(SagaStatus.COMPENSATING);

            WalletCompensationEvent compensationEvent =
                    new WalletCompensationEvent(
                            tx.getId(),
                            tx.getSenderWalletId(),
                            tx.getAmount()
                    );

            kafkaTemplate.send(
                    "wallet-compensation-requested",
                    tx.getId().toString(),
                    compensationEvent
            );
        }

        saga.setUpdatedAt(Instant.now());
        sagaRepository.save(saga);
        transactionRepository.save(tx);
    }
}
