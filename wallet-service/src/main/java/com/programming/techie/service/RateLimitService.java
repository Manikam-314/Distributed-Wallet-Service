package com.programming.techie.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private static final String RATE_PREFIX = "ratelimit:wallet:";
    private static final int LIMIT = 10;

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkRateLimit(Long walletId) {

        String key = RATE_PREFIX + walletId;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        System.out.println("RATE LIMIT COUNT: " + count);

        if (count > LIMIT) {
            throw new RuntimeException("Too many requests. Try again later.");
        }
    }
}
