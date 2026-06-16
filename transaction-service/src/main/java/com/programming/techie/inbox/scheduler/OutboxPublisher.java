package com.programming.techie.inbox.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.events.TransactionCreatedEvent;
import com.programming.techie.inbox.entity.OutboxEvent;
import com.programming.techie.inbox.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    // runs every 500ms to ensure faster responsiveness in microservices
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishEvents() {

        // Polling 10 events at a time with SKIP LOCKED for concurrency (Task 15)
        List<OutboxEvent> events = outboxRepository.findTop10ByPublishedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) return;

        System.out.println("📦 Processing " + events.size() + " events from outbox...");

        for (OutboxEvent event : events) {
            try {
                System.out.println("Processing event " + event.getId() + " for topic " + event.getTopic());
                // ✅ Parse JSON into precise event type based on topic
                Object payload = null;
                switch(event.getTopic()) {
                    case "transaction-created":
                        payload = objectMapper.readValue(event.getPayload(), TransactionCreatedEvent.class);
                        break;
                    case "wallet-credit":
                        payload = objectMapper.readValue(event.getPayload(), com.programming.techie.events.WalletCreditEvent.class);
                        break;
                    case "wallet-compensation-requested":
                        payload = objectMapper.readValue(event.getPayload(), com.programming.techie.events.WalletCompensationEvent.class);
                        break;
                    default:
                        payload = event.getPayload(); // fallback to String
                }

                // ✅ Send correct event type
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getAggregateId(),   // partition key
                        payload
                ).get(); // Block to ensure send is successful before marking as published

                // ✅ Mark as published
                event.setPublished(true);
                outboxRepository.save(event);

                System.out.println("✅ Event published successfully: " + event.getId());

            } catch (Exception e) {
                System.err.println("❌ ERROR: Failed to publish outbox event " + event.getId());
                e.printStackTrace(); // Log full stack trace to terminal for debugging
                // We don't mark as published, it will be retried in next poll
            }
        }
    }
}
