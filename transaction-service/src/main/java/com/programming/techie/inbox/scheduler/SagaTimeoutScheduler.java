package com.programming.techie.inbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.events.WalletCompensationEvent;
import com.programming.techie.events.WalletCreditEvent;
import com.programming.techie.inbox.entity.OutboxEvent;
import com.programming.techie.inbox.repository.OutboxRepository;
import com.programming.techie.saga.entity.TransferSaga;
import com.programming.techie.saga.repository.TransferSagaRepository;
import com.programming.techie.saga.status.SagaStatus;
import com.programming.techie.entity.Transaction;
import com.programming.techie.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaTimeoutScheduler {

    private final TransferSagaRepository sagaRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000) // Run every 60 seconds
    @Transactional
    public void recoverStalledSagas() {
        log.info("🔍 Scanning for stuck Sagas...");
        Instant threshold = Instant.now().minus(2, ChronoUnit.MINUTES);
        
        List<SagaStatus> targetStatuses = Arrays.asList(
            SagaStatus.INITIATED,      // Unknown state
            SagaStatus.DEBIT_SUCCESS,  // Debit done, credit not done
            SagaStatus.COMPENSATING    // Credit failed, compensation pending
        );

        for (SagaStatus status : targetStatuses) {
            List<TransferSaga> stuckSagas = sagaRepository.findByStatusAndUpdatedAtBefore(status, threshold);
            
            for (TransferSaga saga : stuckSagas) {
                log.warn("🚨 Found stuck Saga: ID {}, Status {}", saga.getId(), saga.getStatus());

                Integer retries = saga.getRetryCount() != null ? saga.getRetryCount() : 0;

                if (retries >= 3) {
                    log.error("❌ Saga {} exceeded retry limit. Marking as DEAD.", saga.getId());
                    saga.setStatus(SagaStatus.DEAD);
                    saga.setUpdatedAt(Instant.now());
                    
                    Transaction tx = transactionRepository.findById(saga.getTransactionId()).orElse(null);
                    if (tx != null) {
                        tx.setStatus("DEAD_SAGA");
                        transactionRepository.save(tx);
                    }
                    sagaRepository.save(saga);
                    continue;
                }

                saga.setRetryCount(retries + 1);
                saga.setUpdatedAt(Instant.now());
                sagaRepository.save(saga);

                Transaction tx = transactionRepository.findById(saga.getTransactionId()).orElse(null);
                if (tx == null) continue;

                // CASE 1: Debit done, Credit not done -> Trigger retry event (wallet-credit)
                if (saga.getStatus() == SagaStatus.DEBIT_SUCCESS) {
                    log.info("🔄 RECOVERING: Retrying wallet-credit for Tx {}", tx.getId());
                    WalletCreditEvent creditEvent = new WalletCreditEvent(tx.getId(), tx.getReceiverWalletId(), tx.getAmount());
                    saveOutbox("wallet-credit", tx.getId().toString(), creditEvent);
                }
                
                // CASE 2: Credit failed (or Compensation pending) -> Trigger compensation (wallet-refund)
                else if (saga.getStatus() == SagaStatus.COMPENSATING) {
                    log.info("🔄 RECOVERING: Retrying wallet-compensation for Tx {}", tx.getId());
                    WalletCompensationEvent compEvent = new WalletCompensationEvent(tx.getId(), tx.getSenderWalletId(), tx.getAmount());
                    saveOutbox("wallet-compensation-requested", tx.getId().toString(), compEvent);
                }

                // CASE 3: Unknown state -> Mark as FAILED + log for audit
                else if (saga.getStatus() == SagaStatus.INITIATED) {
                    log.error("⚠️ UNKNOWN STATE: Saga {} stuck in INITIATED. Manual audit required.", saga.getId());
                    saga.setStatus(SagaStatus.FAILED);
                    tx.setStatus("FAILED");
                    sagaRepository.save(saga);
                    transactionRepository.save(tx);
                }
            }
        }
    }
    
    private void saveOutbox(String topic, String aggregateId, Object payloadObj) {
        try {
            String payload = objectMapper.writeValueAsString(payloadObj);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("TRANSACTION_RECOVERY")
                    .aggregateId(aggregateId)
                    .payload(payload)
                    .topic(topic)
                    .published(false)
                    .createdAt(Instant.now())
                    .build();
            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event during recovery", e);
        }
    }
}
