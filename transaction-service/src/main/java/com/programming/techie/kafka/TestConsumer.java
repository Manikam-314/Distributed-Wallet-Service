package com.programming.techie.kafka;

import org.springframework.stereotype.Component;

@Component
public class TestConsumer {

    @KafkaListener(topics = "test-topic", groupId = "test-group")
    public void listen(String message) {
        System.out.println("Received: " + message);
    }
}