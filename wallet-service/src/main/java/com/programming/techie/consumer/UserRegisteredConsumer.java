package com.programming.techie.consumer;

import com.programming.techie.events.UserRegisteredEvent;
import com.programming.techie.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredConsumer {

    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-registered-topic", groupId = "wallet-group")
    public void consume(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: {}", event);
        try {
            log.info("Processing wallet creation for userId: {}", event.getUserId());
            walletService.createWallet(event.getUserId());
            log.info("Successfully processed wallet creation for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process user registration", e); 
        }
    }
}
