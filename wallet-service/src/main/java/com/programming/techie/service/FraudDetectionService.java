package com.programming.techie.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FraudDetectionService {

    private static final String FRAUD_PREFIX = "fraud:wallet:";

    private final RedisTemplate<String, Object> redisTemplate;

    public FraudDetectionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkFraud(Long walletId) {

        String key = FRAUD_PREFIX + walletId;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, 300, TimeUnit.SECONDS);        }
        System.out.println("REDIS KEY CREATED: " + key);
        System.out.println("FRAUD CHECK → wallet " + walletId + " count = " + count);

        if (count > 15) {
            throw new RuntimeException("Fraud detected: Too many requests");
        }
    }
}
