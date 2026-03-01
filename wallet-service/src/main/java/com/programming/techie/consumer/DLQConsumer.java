package com.programming.techie.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.techie.entity.FailedEvent;
import com.programming.techie.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DLQConsumer {

    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;   // ⭐ inject this
    @KafkaListener(topics = "transaction-created.DLT", groupId = "wallet-dlq-group")
    public void consumeDLQ(
            ConsumerRecord<String, Object> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.DELIVERY_ATTEMPT, required = false) Integer attempts    ) {

        try {
            System.out.println("🔥 DLQ EVENT RECEIVED");

            String payload = objectMapper.writeValueAsString(record.value());

            FailedEvent failedEvent = FailedEvent.builder()
                    .originalTopic(topic)
                    .payload(payload)
                    .errorReason("Max retries exceeded")
                    .retryCount(attempts != null ? attempts : 3)
                    .failedAt(Instant.now())
                    .build();

            failedEventRepository.save(failedEvent);

            System.out.println("DLQ event stored in DB");

        } catch (Exception e) {
            System.out.println("Error storing DLQ event: " + e.getMessage());
        }
    }
}