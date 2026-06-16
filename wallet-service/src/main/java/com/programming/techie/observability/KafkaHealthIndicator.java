package com.programming.techie.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Phase 7: Custom Health Indicator for Kafka broker connectivity.
 * Shows up in /actuator/health as "kafkaCustom".
 */
@Component("kafkaCustom")
public class KafkaHealthIndicator implements HealthIndicator {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000"))) {

            adminClient.listTopics().names().get(5, TimeUnit.SECONDS);

            return Health.up()
                    .withDetail("kafka", "Connected")
                    .withDetail("bootstrapServers", bootstrapServers)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("kafka", "Connection failed")
                    .withDetail("bootstrapServers", bootstrapServers)
                    .withException(e)
                    .build();
        }
    }
}
