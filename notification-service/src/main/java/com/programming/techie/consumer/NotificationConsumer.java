package com.programming.techie.consumer;

import com.programming.techie.events.NotificationEvent;
import com.programming.techie.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SmsService smsService;
    private final com.programming.techie.service.EmailService emailService;
    private final com.programming.techie.repository.NotificationRepository notificationRepository;

    @KafkaListener(topics = "notificationTopic", groupId = "notificationGroup")
    public void consume(NotificationEvent event) {
        log.info("Received Notification Event for mobile: {}, email: {}, channel: {}, message: {}",
                event.getMobileNumber(), event.getEmail(), event.getDeliveryChannel(), event.getMessage());

        // ⭐ SAVE TO DATABASE FOR WEB UI
        com.programming.techie.entity.InAppNotification inApp = com.programming.techie.entity.InAppNotification.builder()
                .email(event.getEmail())
                .mobileNumber(event.getMobileNumber())
                .message(event.getMessage())
                .build();
        notificationRepository.save(inApp);
        log.info("Saved In-App Notification for web UI");


        String channel = event.getDeliveryChannel();
        if (channel == null) channel = "BOTH"; // Default fallback

        // Send SMS
        if (("BOTH".equalsIgnoreCase(channel) || "SMS".equalsIgnoreCase(channel)) 
            && event.getMobileNumber() != null && !event.getMobileNumber().isEmpty()) {
            smsService.sendSms(event.getMobileNumber(), event.getMessage());
        }

        // Send Email
        if (("BOTH".equalsIgnoreCase(channel) || "EMAIL".equalsIgnoreCase(channel)) 
            && event.getEmail() != null && !event.getEmail().isEmpty()) {
            emailService.sendEmail(event.getEmail(), event.getMessage());
        }
    }
}
