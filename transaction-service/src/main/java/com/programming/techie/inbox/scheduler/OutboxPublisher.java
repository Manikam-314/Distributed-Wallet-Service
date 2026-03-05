package com.programming.techie.inbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.inbox.entity.OutboxEvent;
import com.programming.techie.inbox.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // runs every 5 seconds
    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {

        List<OutboxEvent> events = outboxRepository.findByPublishedFalse();

        for (OutboxEvent event : events) {
            try {

                // ✅ Convert JSON → TransactionCreatedEvent
                TransactionCreatedEvent payload =
                        objectMapper.readValue(
                                event.getPayload(),
                                TransactionCreatedEvent.class
                        );

                // ✅ Send correct event type
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getAggregateId(),   // partition key
                        payload
                );

                // ✅ Mark as published
                event.setPublished(true);
                outboxRepository.save(event);

                System.out.println("✅ Event published from outbox: " + event.getId());

            } catch (Exception e) {
                System.out.println("❌ Failed to publish outbox event: " + e.getMessage());
            }
        }
    }
}
