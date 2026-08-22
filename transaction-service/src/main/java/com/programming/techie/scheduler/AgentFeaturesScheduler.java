package com.programming.techie.scheduler;

import com.programming.techie.client.AuthClient;
import com.programming.techie.dto.UserDTO;
import com.programming.techie.entity.RecurringPayment;
import com.programming.techie.entity.ScheduledPayment;
import com.programming.techie.events.NotificationEvent;
import com.programming.techie.repository.RecurringPaymentRepository;
import com.programming.techie.repository.ScheduledPaymentRepository;
import com.programming.techie.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgentFeaturesScheduler {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final RecurringPaymentRepository recurringPaymentRepository;
    private final TransactionService transactionService;
    private final AuthClient authClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedRate = 10000)
    public void sweepAgentFeatures() {
        processScheduledPayments();
        processRecurringPayments();
    }

    private void processScheduledPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledPayment> duePayments = scheduledPaymentRepository
                .findByStatusAndExecutionTimeLessThanEqual("PENDING", now);

        if (!duePayments.isEmpty()) {
            log.info("Found {} due scheduled payments at {}", duePayments.size(), now);
        }

        for (ScheduledPayment sp : duePayments) {
            try {
                // If condition exists and is not met, trigger reminder instead
                if (sp.getConditionMinAmount() != null && !sp.isConditionMet()) {
                    log.info("Condition not met for scheduled payment {}. Triggering reminder notification.", sp.getId());
                    sp.setStatus("REMINDED");
                    scheduledPaymentRepository.save(sp);

                    sendReminderNotification(sp);
                    continue;
                }

                String idempotencyKey = "SCH-PAY-" + sp.getId();
                log.info("Executing scheduled payment ID {} via Saga, key={}", sp.getId(), idempotencyKey);
                
                transactionService.transfer(
                        sp.getSenderWalletId(),
                        sp.getReceiverWalletId(),
                        sp.getAmount(),
                        idempotencyKey,
                        sp.getUserId()
                );

                sp.setStatus("COMPLETED");
                scheduledPaymentRepository.save(sp);
                log.info("Successfully executed scheduled payment ID {}", sp.getId());

            } catch (Exception e) {
                log.error("Failed to execute scheduled payment ID {}: {}", sp.getId(), e.getMessage());
                sp.setStatus("FAILED");
                sp.setFailReason(e.getMessage());
                scheduledPaymentRepository.save(sp);
            }
        }
    }

    private void processRecurringPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<RecurringPayment> duePayments = recurringPaymentRepository
                .findByStatusAndNextExecutionTimeLessThanEqual("ACTIVE", now);

        if (!duePayments.isEmpty()) {
            log.info("Found {} due recurring payments at {}", duePayments.size(), now);
        }

        for (RecurringPayment rp : duePayments) {
            LocalDateTime executionTime = rp.getNextExecutionTime();
            try {
                // Unique idempotency key based on recurring ID and current date
                String dateStr = executionTime.toLocalDate().toString();
                String idempotencyKey = "REC-PAY-" + rp.getId() + "-" + dateStr;
                
                log.info("Executing recurring payment ID {} for cycle {}, key={}", rp.getId(), dateStr, idempotencyKey);

                transactionService.transfer(
                        rp.getSenderWalletId(),
                        rp.getReceiverWalletId(),
                        rp.getAmount(),
                        idempotencyKey,
                        rp.getUserId()
                );

                log.info("Successfully executed cycle of recurring payment ID {}", rp.getId());

            } catch (Exception e) {
                log.error("Failed cycle of recurring payment ID {} for cycle: {}", rp.getId(), e.getMessage());
            } finally {
                // Advance execution time regardless of success/failure to keep schedule intact
                LocalDateTime nextTime = advanceTime(rp.getNextExecutionTime(), rp.getFrequency());
                rp.setNextExecutionTime(nextTime);
                recurringPaymentRepository.save(rp);
                log.info("Advanced recurring payment ID {} schedule to next execution time: {}", rp.getId(), nextTime);
            }
        }
    }

    private LocalDateTime advanceTime(LocalDateTime current, String frequency) {
        if (frequency == null) {
            return current.plusMonths(1);
        }
        return switch (frequency.toUpperCase()) {
            case "DAILY" -> current.plusDays(1);
            case "WEEKLY" -> current.plusWeeks(1);
            case "MONTHLY" -> current.plusMonths(1);
            default -> current.plusMonths(1);
        };
    }

    private void sendReminderNotification(ScheduledPayment sp) {
        try {
            UserDTO user = authClient.getUser(sp.getUserId());
            if (user != null) {
                String message = sp.getReminderMessage() != null ? sp.getReminderMessage() : 
                        String.format("⏰ REMINDER: Your scheduled payment of ₹%.2f failed to execute because condition was not met.", sp.getAmount());

                NotificationEvent event = NotificationEvent.builder()
                        .mobileNumber(user.getMobileNumber())
                        .email(user.getEmail())
                        .message(message)
                        .deliveryChannel("BOTH")
                        .build();

                kafkaTemplate.send("notificationTopic", event);
                log.info("Sent conditional scheduled payment reminder to user: {}", user.getName());
            }
        } catch (Exception e) {
            log.error("Failed to send scheduled payment notification for user ID {}: {}", sp.getUserId(), e.getMessage());
        }
    }
}
