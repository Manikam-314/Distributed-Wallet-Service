package com.programming.techie.kafka;

import com.programming.techie.entity.ConditionalTrigger;
import com.programming.techie.entity.ScheduledPayment;
import com.programming.techie.events.WalletCreditEvent;
import com.programming.techie.repository.ConditionalTriggerRepository;
import com.programming.techie.repository.ScheduledPaymentRepository;
import com.programming.techie.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgentTriggerConsumer {

    private final ConditionalTriggerRepository conditionalTriggerRepository;
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final TransactionService transactionService;

    @KafkaListener(
            topics = "wallet-credit",
            groupId = "agent-trigger-group"
    )
    public void consume(WalletCreditEvent event) {
        log.info("[AgentTriggerConsumer] Credit event detected: walletId={}, amount={}", event.getWalletId(), event.getAmount());

        Long creditedWalletId = event.getWalletId();
        java.math.BigDecimal creditAmount = event.getAmount();

        if (creditedWalletId == null || creditAmount == null) {
            return;
        }

        // 1. Process Conditional Triggers (e.g. pay Mom after salary comes)
        List<ConditionalTrigger> pendingTriggers = conditionalTriggerRepository.findBySenderWalletIdAndStatus(creditedWalletId, "PENDING");
        for (ConditionalTrigger ct : pendingTriggers) {
            if (creditAmount.compareTo(ct.getMinAmount()) >= 0) {
                try {
                    String idempotencyKey = "COND-TRG-" + ct.getId();
                    log.info("[AgentTriggerConsumer] Match found for ConditionalTrigger ID {}. Executing transfer.", ct.getId());
                    
                    transactionService.transfer(
                            ct.getSenderWalletId(),
                            ct.getReceiverWalletId(),
                            ct.getAmount(),
                            idempotencyKey,
                            ct.getUserId()
                    );

                    ct.setStatus("TRIGGERED");
                    conditionalTriggerRepository.save(ct);
                    log.info("[AgentTriggerConsumer] Trigger ID {} successfully executed and completed.", ct.getId());

                } catch (Exception e) {
                    log.error("[AgentTriggerConsumer] Failed to execute trigger ID {}: {}", ct.getId(), e.getMessage());
                    ct.setStatus("FAILED");
                    ct.setFailReason(e.getMessage());
                    conditionalTriggerRepository.save(ct);
                }
            }
        }

        // 2. Update conditionMet for scheduled payments (e.g. check salary for electricity bill)
        List<ScheduledPayment> pendingScheduledPayments = scheduledPaymentRepository
                .findBySenderWalletIdAndStatusAndConditionMinAmountIsNotNull(creditedWalletId, "PENDING");

        for (ScheduledPayment sp : pendingScheduledPayments) {
            if (!sp.isConditionMet() && creditAmount.compareTo(sp.getConditionMinAmount()) >= 0) {
                log.info("[AgentTriggerConsumer] Credit amount ₹{} meets condition threshold ₹{} for scheduled payment ID {}. Marking conditionMet = true.",
                        creditAmount, sp.getConditionMinAmount(), sp.getId());
                sp.setConditionMet(true);
                scheduledPaymentRepository.save(sp);
            }
        }
    }
}
