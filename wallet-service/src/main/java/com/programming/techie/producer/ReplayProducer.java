package com.programming.techie.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplayProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void replay(String topic, String payload) {
        kafkaTemplate.send(topic, payload);
        System.out.println("Event replayed to topic: " + topic);
    }
}
