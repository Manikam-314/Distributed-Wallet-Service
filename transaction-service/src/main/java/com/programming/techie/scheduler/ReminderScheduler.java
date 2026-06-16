package com.programming.techie.scheduler;

import com.programming.techie.client.AuthClient;
import com.programming.techie.dto.UserDTO;
import com.programming.techie.entity.MoneyRequest;
import com.programming.techie.entity.RequestStatus;
import com.programming.techie.events.NotificationEvent;
import com.programming.techie.repository.MoneyRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderScheduler {

    private final MoneyRequestRepository moneyRequestRepository;
    private final AuthClient authClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Runs every 10 seconds to check for due payments.
     */
    @Scheduled(fixedRate = 10000)
    public void sendReminders() {
        String nowStr = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString();
        
        // Find pending requests that are due (or overdue) and haven't been notified yet
        List<MoneyRequest> dueRequests = moneyRequestRepository.findByStatusAndReminderSentFalseAndExtractedDueDateLessThanEqual(
                RequestStatus.PENDING, nowStr);
                
        if (!dueRequests.isEmpty()) {
            log.info("Found {} requests due for reminder (Now: {})", dueRequests.size(), nowStr);
        }
        
        for (MoneyRequest req : dueRequests) {
            try {
                UserDTO recipient = authClient.getUser(req.getRecipientId());
                UserDTO requester = authClient.getUser(req.getRequesterId());
                
                if (recipient != null) {
                    NotificationEvent event = NotificationEvent.builder()
                            .mobileNumber(recipient.getMobileNumber())
                            .email(recipient.getEmail())
                            .message(String.format("⏰ SMART REMINDER: You have a pending request of ₹%.2f from %s due now!", 
                                    req.getAmount(),
                                    requester != null ? requester.getName() : "a user"))
                            .deliveryChannel("BOTH")
                            .build();
                            
                    kafkaTemplate.send("notificationTopic", event);
                    
                    // Mark as sent so we don't spam
                    req.setReminderSent(true);
                    moneyRequestRepository.save(req);
                    
                    log.info("Sent smart reminder for request ID {} to user {}", req.getId(), recipient.getName());
                }
            } catch (Exception e) {
                log.error("Failed to send reminder for request {}: {}", req.getId(), e.getMessage());
            }
        }
    }
}
