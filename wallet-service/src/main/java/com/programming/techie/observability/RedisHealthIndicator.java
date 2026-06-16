package com.programming.techie.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Phase 7: Custom Health Indicator for Redis connectivity.
 * Shows up in /actuator/health as "redisCustom".
 */
@Component("redisCustom")
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try {
            redisConnectionFactory.getConnection().ping();
            return Health.up()
                    .withDetail("redis", "Connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Connection failed")
                    .withException(e)
                    .build();
        }
    }
}
