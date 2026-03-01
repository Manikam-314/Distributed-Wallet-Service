package com.programming.techie.config;

import com.programming.techie.events.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class  KafkaConsumerConfig {

    // ⭐ CONSUMER FACTORY
    @Bean
    public ConsumerFactory<String, TransactionCreatedEvent> consumerFactory() {

        JsonDeserializer<TransactionCreatedEvent> deserializer =
                new JsonDeserializer<>(TransactionCreatedEvent.class);

        deserializer.addTrustedPackages("*");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "wallet-group");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> dlqKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(dlqKafkaTemplate);

        FixedBackOff backOff = new FixedBackOff(2000L, 3);

        return new DefaultErrorHandler(recoverer, backOff);
    }
// ⭐ LISTENER FACTORY
@Bean
public ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent>
kafkaListenerContainerFactory(DefaultErrorHandler errorHandler) {

    ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory());

    // VERY IMPORTANT
    factory.setCommonErrorHandler(errorHandler);

    return factory;
}
}

