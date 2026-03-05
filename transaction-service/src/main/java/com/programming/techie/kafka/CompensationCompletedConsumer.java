package com.programming.techie.kafka;

import com.programming.techie.events.CompensationCompletedEvent;
import com.programming.techie.saga.entity.TransferSaga;
import com.programming.techie.saga.repository.TransferSagaRepository;
import com.programming.techie.saga.status.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CompensationCompletedConsumer {

    private final TransferSagaRepository sagaRepository;

    @KafkaListener(
            topics = "compensation-completed",
            groupId = "transaction-group"
    )
    public void consume(CompensationCompletedEvent event) {

        TransferSaga saga = sagaRepository
                .findByTransactionId(event.getTransactionId())
                .orElseThrow();

        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setUpdatedAt(Instant.now());

        sagaRepository.save(saga);

        System.out.println("SAGA COMPENSATED → " + event.getTransactionId());
    }
}
